package org.meandre.tools.wiki;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.meandre.core.repository.DataPortDescription;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.PropertiesDescriptionDefinition;
import org.meandre.core.repository.RepositoryImpl;

import com.atlassian.www._package.com_atlassian_confluence_rpc_soap_beans.RemotePage;
import com.atlassian.www.software.confluence.$Proxy42.ConfluenceServiceLocator;
import com.atlassian.www.software.confluence.$Proxy42.ConfluenceSoapService;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Tool that generates documentation for components and uploads it to the Confluence wiki; <br/>
 * the tool also creates an index page containing a list of all components; <br/>
 * the list can be sorted by clicking on the table headers (except on Description)
 *
 * @author Boris Capitanu
 *
 */
public class WikiCreateComponentPages {

    private static final Logger _logger = Logger.getLogger(WikiCreateComponentPages.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    private static ConfluenceSoapService _service;
    private static String _token;

    public static void main(String[] args) {
        if (args == null || args.length == 0)
            args = new String[] { "--help" };

        // Set up a simple configuration that logs on the console
//        PatternLayout logLayout = new PatternLayout("%5p: %m%n");
//        ConsoleAppender consoleAppender = new ConsoleAppender(logLayout);
//        _logger.removeAllAppenders();
//        _logger.addAppender(consoleAppender);
        _logger.setLevel(Level.INFO);

        // Parse command line arguments
        JSAPResult jsapResult = parseArguments(args);

        // Extract the argument values
        String descriptorsDir = jsapResult.getString("descriptorsDir");
        String user = jsapResult.getString("user");
        String password = jsapResult.getString("password");
        String address = jsapResult.getString("server") + jsapResult.getString("soap");
        String space = jsapResult.getString("space");
        String title = jsapResult.getString("title");
        String parent = jsapResult.getString("parent");

        // Check whether the descriptors directory really exists
        if (!new File(descriptorsDir).exists())
            fail(new FileNotFoundException("The directory '" + descriptorsDir + "' does not exist"));

        // Connect to Confluence and log in
        ConfluenceServiceLocator confluenceServiceLocator = new ConfluenceServiceLocator();
        confluenceServiceLocator.setConfluenceSoapServiceEndpointAddress(address);
        try {
        	_service = confluenceServiceLocator.getConfluenceSoapService();
        	_token = _service.login(user, password);
        }
        catch (Exception exception) {
            fail(new Exception("Unable to log in to server: " + address +
            		". Verify your server, user id, and password are correct. "));
        }

        _logger.info("Successful login to " + address);

        // Find all the component descriptors
        _logger.info("Searching for descriptors in " + descriptorsDir);
        List<String> componentDescriptorFileNames = getFiles(descriptorsDir, new FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".rdf");
            }
        }, true);

        _logger.info("Found " + componentDescriptorFileNames.size() + " component descriptors");

        // Create a repository containing all the components
        RepositoryImpl repository = createComponentRepository(componentDescriptorFileNames);

        // Create the index page and all the component pages
        try {
			createIndexPage(repository, space, title, parent);
			createComponentPages(repository, space, title);
		} catch (Exception e) {
			fail(e);
		}
    }

    /**
     * Creates the index page containing a list of all available components
     *
     * @param repository The repository for the components
     * @param space The space that will host the index page
     * @param title The title of the index page
     * @param parent The id of the parent page to link to (or null if no parent)
     * @throws java.rmi.RemoteException Thrown if a communication error occured
     * @throws SoapClientException Thrown if a protocol error occured
     */
    private static void createIndexPage(RepositoryImpl repository, String space, String title, String parent)
    	throws java.rmi.RemoteException, SoapClientException {

        boolean createdNew = false;
        boolean setParent = false;

        RemotePage page;
        try {
        	page = getPage(space, title);
        }
        catch (SoapClientException e) {
        	page = new RemotePage();
        	createdNew = true;
        }

        if (parent != null && parent.length() > 0) {
        	try {
        		page.setParentId(getPage(space, parent).getId());
        		setParent = true;
        	}
        	catch (SoapClientException ignore) {
        		// Do nothing if parent doesn't exist
        	}
        }

        Calendar calendar = Calendar.getInstance();

        if (createdNew) {
        	page.setSpace(space);
            page.setTitle(title);
            page.setCreated(calendar);
        }

        page.setContent(generateRepositoryIndexContent(repository, space));
        page.setModified(calendar);

        page = _service.storePage(_token, page);

        _logger.info((createdNew ? "Created index page " : "Updated index page ") + page.getUrl() +
        		" (id " + page.getId() + ", version " + page.getVersion() + ")" +
        		(setParent ? " as child of '" + parent : "") + "'");
	}

    /**
     * Parses the command line arguments
     *
     * @param args The command line arguments
     * @return A JSAPResult object containing the parsed arguments
     */
	private static JSAPResult parseArguments(String[] args) {
	    JSAPResult result = null;

	    String generalHelp = "Creates a wiki documentation page for each SEASR component containing " +
	                         "a description of what the component does, what it's inputs, outputs, " +
	                         "and properties are, with descriptions for each. Also creates a 'main " +
	                         "page' containing a list of all the available components, with links to each.";

	    try {
	        SimpleJSAP jsap = new SimpleJSAP("WikiCreateComponentPages", generalHelp,
	           new Parameter[] {
	                   new FlaggedOption("server", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "server", "Confluence server URL.")
	                 , new FlaggedOption("user", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "user", "Confluence user name.")
	                 , new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password", "Confluence user password.")
	                 , new FlaggedOption("soap", JSAP.STRING_PARSER, "/rpc/soap/confluenceservice-v1", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "soap", "Soap address extension.")
	                 , new FlaggedOption("descriptorsDir", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "descriptorsDir", "Directory containing the component descriptors.")
	                 , new FlaggedOption("title", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NO_SHORTFLAG, "title", "Index page title.")
	                 , new FlaggedOption("space", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NO_SHORTFLAG, "space", "Space location key.")
	                 , new FlaggedOption("parent", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "parent", "The parent page for the index.")
	                   //, new FlaggedOption("component", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'c', "component", "The component name whose documentation to regenerate")
	           });

	        result = jsap.parse(args);
	        if (jsap.messagePrinted())
	            System.exit(-1);
	    }
	    catch (JSAPException e) {
	        fail(e);
	    }

	    return result;
	}

	/**
	 * Creates wiki pages for all components in the repository
	 *
	 * @param repository The repository
	 * @param space The space that will host the pages
	 * @param parent The parent to link to (or null if none)
	 * @throws java.rmi.RemoteException Thrown if a communication error occured
	 * @throws SoapClientException Thrown if a protocol error occured
	 */
	private static void createComponentPages(RepositoryImpl repository, String space, String parent)
		throws java.rmi.RemoteException, SoapClientException {

    	for (Resource component : repository.getAvailableExecutableComponents())
            createPageForComponent(repository.getExecutableComponentDescription(component), space, parent);
    }

	/**
	 * Creates a single wiki page for a component
	 *
	 * @param compDesc The component description
	 * @param space The space that will host the page
	 * @param parent The parent to link to (or null if none)
	 * @throws java.rmi.RemoteException Thrown if a communication error occured
	 * @throws SoapClientException Thrown if a protocol error occured
	 */
    private static void createPageForComponent(ExecutableComponentDescription compDesc, String space, String parent)
    	throws java.rmi.RemoteException, SoapClientException {

        boolean createdNew = false;
        boolean setParent = false;
        boolean setLabels = false;

        String title = compDesc.getName();

        RemotePage page;
        try {
        	page = getPage(space, title);
        }
        catch (SoapClientException e) {
        	page = new RemotePage();
        	createdNew = true;
        }

        if (parent != null && parent.length() > 0) {
        	try {
        		page.setParentId(getPage(space, parent).getId());
        		setParent = true;
        	}
        	catch (SoapClientException ignore) {
        		// Do nothing if parent doesn't exist
        	}
        }

        Calendar calendar = Calendar.getInstance();

        if (createdNew) {
        	page.setSpace(space);
            page.setTitle(title);
            page.setCreated(calendar);
        }

        page.setContent(generateContentForComponent(compDesc));
        page.setModified(calendar);

        page = _service.storePage(_token, page);

        String tags = compDesc.getTags().toString();
        if (tags.length() > 0) {
        	try {
        		addLabels(tags, page.getId());
        		setLabels = true;
        	}
        	catch (SoapClientException e) {
        	}
        }

        _logger.info((createdNew ? "Created page " : "Updated page ") + page.getUrl() +
        		" (id " + page.getId() + ", version " + page.getVersion() + ")" +
        		(setParent ? " as child of '" + parent : "") + "'" +
        		(setLabels ? " with labels: " + tags : ""));
	}

    /**
     * Associates tags with a page
     *
     * @param labels The space-separated list of tags
     * @param id The page id
     * @throws SoapClientException Thrown if a protocol error occured
     */
    private static void addLabels(String labels, long id) throws SoapClientException {
    	String[] list = labels.split(",");
        try {
        	for (String label : list)
        		_service.addLabelByName(_token, label.trim(), id);
        }
        catch (java.rmi.RemoteException exception) {
            String message = "Error adding labels to object with id: " + id;
            throw new SoapClientException(message);
        }
    }

    /**
     * Retrieves a page from the wiki
     *
     * @param space The space hosting the page
     * @param title The title of the page
     * @return The remote page
     * @throws SoapClientException Thrown if a protocol error occured
     */
	private static RemotePage getPage(String space, String title) throws SoapClientException
    {
        try {
            return _service.getPage(_token, space, title);
        }
        catch (java.rmi.RemoteException exception) {
            String message = "Page '" + title + "' not found in space '" + space + "'";
            throw new SoapClientException(message);
        }
    }

	/**
	 * Creates a component repository based on a list of component descriptor files
	 *
	 * @param componentDescriptorFileNames The list of files containing component descriptors
	 * @return The repository
	 */
	private static RepositoryImpl createComponentRepository(List<String> componentDescriptorFileNames) {
        Model model = ModelFactory.createDefaultModel();
        for (String descriptorFileName : componentDescriptorFileNames) {
            try {
                model.read(new FileInputStream(descriptorFileName), null);
            }
            catch (FileNotFoundException e) {
                fail(e);
            }
        }

        return new RepositoryImpl(model);
    }

//    private static File createTempDir() throws IOException {
//        File tempFile = File.createTempFile("tmpDir", "");
//        if (!tempFile.delete()) throw new IOException();
//        if (!tempFile.mkdir()) throw new IOException();
//
//        return tempFile;
//    }
//
//    private static boolean deleteDirectory(File path) {
//        if (path.exists()) {
//            for (File file : path.listFiles()) {
//                if (file.isDirectory())
//                    deleteDirectory(file);
//                else
//                    file.delete();
//            }
//        }
//
//        return path.delete();
//    }

	/**
	 * Creates the content for the index page
	 *
	 * @param repository The component repository
	 * @param space The space that will hold the index page
	 * @return The wiki content
	 */
	private static String generateRepositoryIndexContent(RepositoryImpl repository, String space) {
        Set<Resource> components = repository.getAvailableExecutableComponents();

        /**
         * Create the 'AvailableComponents' page
         */
        StringBuilder sb = new StringBuilder();
        sb.append("{table-plus:").
                append("columnTypes=S,S,X,D|").
                append("enableSorting=true|").
                append("enableHighlighting=true|").
                append("sortColumn=Package|").
                append("autoNumber=true}").append(NEWLINE);
        sb.append("||Name||Package||Description||Creation date||").append(NEWLINE);

        for (Resource component : components) {
            ExecutableComponentDescription compDesc = repository.getExecutableComponentDescription(component);

            sb.append("|[").append(compDesc.getName()).append("|").append(space).append(":").append(compDesc.getName()).append("]");
            String packageName = getComponentPackage(compDesc);
            sb.append("|").append(packageName);
            sb.append("|").append("{html}<div>").append(compDesc.getDescription()).append("</div>{html}");
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            sb.append("|").append(dateFormat.format(compDesc.getCreationDate()));
            sb.append("|").append(NEWLINE);
        }

        sb.append("{table-plus}");

        return sb.toString();
    }

	/**
	 * Creates the content for a component
	 *
	 * @param compDesc The component description
	 * @return The wiki content
	 */
    private static String generateContentForComponent(ExecutableComponentDescription compDesc) {

        //Map<String, String> ioDataTypes = getComponentDataTypes(compDesc);

        StringBuilder sb = new StringBuilder();

        /**
         * Create the 'DETAILS' section
         */
        String author = compDesc.getCreator();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String packageName = getComponentPackage(compDesc);
        sb.append("|Author|").append(author.length() > 0 ? author : "_Not specified_").append("|").append(NEWLINE);
        sb.append("|Creation date|").append(dateFormat.format(compDesc.getCreationDate())).append("|").append(NEWLINE);
        sb.append("|Firing policy|").append(compDesc.getFiringPolicy()).append("|").append(NEWLINE);
        sb.append("|Package|").append(packageName).append("|").append(NEWLINE);

        sb.append(NEWLINE);

        /**
         * Create the 'DESCRIPTION' section
         */
        sb.append("+DESCRIPTION+").append(NEWLINE);
        String description = compDesc.getDescription();
        if (description.length() > 0)
        	sb.append("{html}<div>").append(description).append("</div>{html}");
        else
        	sb.append("_None_");

        sb.append(NEWLINE).append(NEWLINE);

        /**
         * Create 'INPUTS' section
         */
        sb.append("+INPUTS+").append(NEWLINE);

        Set<DataPortDescription> inputs = compDesc.getInputs();
        if (inputs.size() > 0) {
            //sb.append("||Name||Detected Type||Description||Example||").append(NEWLINE);
            sb.append("||Name||Description||Example||").append(NEWLINE);
            for (DataPortDescription input : inputs) {
            	// Name
                sb.append("|")
                	.append("{html}<div>")
                	.append(escapeHtml(input.getName()))
                	.append("</div>{html}");
                // Type
                //sb.append("|_").append(ioDataTypes != null ? (ioDataTypes.get("input: " + input.getName())) : " ").append("_");
                // Description
                sb.append("|")
                	.append("{html}<div>")
                	.append(input.getDescription())
                	.append("</div>{html}");
                // Example
                sb.append("|").append(" ");
                sb.append("|").append(NEWLINE);
            }
        }
        else
            sb.append("_None_").append(NEWLINE);

        sb.append(NEWLINE);

        /**
         * Create 'OUTPUTS' section
         */
        sb.append("+OUTPUTS+").append(NEWLINE);

        Set<DataPortDescription> outputs = compDesc.getOutputs();
        if (outputs.size() > 0) {
            //sb.append("||Name||Detected Type||Description||Example||").append(NEWLINE);
            sb.append("||Name||Description||Example||").append(NEWLINE);
            for (DataPortDescription output : outputs) {
            	// Name
                sb.append("|")
                	.append("{html}<div>")
                	.append(escapeHtml(output.getName()))
                	.append("</div>{html}");
                // Type
                //sb.append("|_").append(ioDataTypes != null ? (ioDataTypes.get("output: " + output.getName())) : " ").append("_");
                // Description
                sb.append("|")
                	.append("{html}<div>")
                	.append(output.getDescription())
                	.append("</div>{html}");
                // Example
                sb.append("|").append(" ");
                sb.append("|").append(NEWLINE);
            }
        }
        else
            sb.append("_None_").append(NEWLINE);

        sb.append(NEWLINE);

        /**
         * Create 'PROPERTIES' section
         */
        sb.append("+PROPERTIES+").append(NEWLINE);

        PropertiesDescriptionDefinition propDesc = compDesc.getProperties();
        Set<String> propertyNames = propDesc.getKeys();
        if (propertyNames.size() > 0) {
            //sb.append("||Name||Type||Description||Default value||").append(NEWLINE);
            sb.append("||Name||Description||Default value||").append(NEWLINE);
            for (String propertyName : propertyNames) {
            	// Name
                sb.append("|")
                	.append("{html}<div>")
                	.append(escapeHtml(propertyName))
                	.append("</div>{html}");
                // Type
                //sb.append("|").append(" ");
                // Description
                sb.append("|")
                	.append("{html}<div>")
                	.append(propDesc.getDescription(propertyName))
                	.append("</div>{html}");
                // Default value
                sb.append("|")
                	.append("{html}<div>")
                	.append(escapeHtml(propDesc.getValue(propertyName)))
                	.append("</div>{html}");
                sb.append("|").append(NEWLINE);
            }
        }
        else
            sb.append("_None_").append(NEWLINE);

        return sb.toString();
    }

    /**
     * Returns the java package of a particular component
     *
     * @param compDesc The component descriptor object
     * @return The java package name
     */
    private static String getComponentPackage(ExecutableComponentDescription compDesc) {
        String packageName = compDesc.getLocation().toString();
        packageName = packageName.substring(packageName.lastIndexOf("/") + 1);
        packageName = packageName.substring(0, packageName.lastIndexOf("."));
        return packageName;
    }

//    private static Map<String, String> getComponentDataTypes(ExecutableComponentDescription compDesc) {
//        Set<RDFNode> contextLocations = compDesc.getContext();
//        if (contextLocations == null) return null;
//
//        Map<String, String> dataTypeMap = new HashMap<String, String>();
////        for (DataPortDescription in : compDesc.getInputs())
////            dataTypeMap.put(in.getName(), "N/A");
////        for (DataPortDescription out : compDesc.getOutputs())
////            dataTypeMap.put(out.getName(), "N/A");
//
//        for (RDFNode contextLocation : contextLocations) {
//            String location = contextLocation.toString();
//            String contextFileName = location.toLowerCase();
//            contextFileName = contextFileName.substring(contextFileName.lastIndexOf('/') + 1).trim();
//
//            if (!(contextFileName.startsWith(getComponentPackage(compDesc)) && contextFileName.endsWith(".jar"))) continue;
//
//            try {
//                WebdavClient davClient = new WebdavClient(location);
//                JarInputStream jarStream = new JarInputStream(davClient.getResourceAsStream(location));
//                JarEntry entry = null;
//                while ((entry = jarStream.getNextJarEntry()) != null) {
//                    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".class")) {
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        byte[] buffer = new byte[1024];
//                        int nread = 0;
//                        while ((nread = jarStream.read(buffer)) > 0)
//                            baos.write(buffer, 0, nread);
//                        byte[] classData = baos.toByteArray();
//                        //FileUtils.writeByteArrayToFile(new File("/tmp/class/" + entry.getName().replaceAll("/", ".")), classData);
//
//                        Map<String, MethodDataType> mdt = buildComponentDataTypeMapFromClass(classData);
//                        for (String s : mdt.keySet()) {
//                            String io = s.substring(0, s.lastIndexOf('_'));
//                            String mode = s.endsWith("Input") ? "input: " : "output: ";
//                            String dataType = TypeUtils.getDataType(mdt.get(s).getVariableDataType());
//                            _logger.debug("    type: " + mode + "  key: " + s + "  value: " + dataType);
//                            if (dataType != null) {
//                                String[] types = dataType.split(",");
//                                StringBuffer sb = new StringBuffer();
//                                sb.append("{html}");
//                                Set<String> dataTypes = new HashSet<String>();
//                                for (String type : types)
//                                    dataTypes.add(type);
//
//                                for (String type : dataTypes)
//                                    sb.append(escapeHtml(type)).append("<br>");
//
//                                sb.append("{html}");
//                                dataTypeMap.put(mode + io, sb.toString());
//                            } else
//                                dataTypeMap.put(mode + io, null);
//
//                        }
//                    }
//                }
//            }
//            catch (MalformedURLException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        return dataTypeMap;
//    }
//
//    private static Map<String, MethodDataType> buildComponentDataTypeMapFromClass(byte[] classBytes) {
//        ClassNode classNodeVisitor = new ClassNode();
//        ClassReader classReader = new ClassReader(classBytes);
//        classReader.accept(classNodeVisitor, 0);
//        ComponentExecuteMethodTransformer cemt = new ComponentExecuteMethodTransformer(null);
//        cemt.transform(classNodeVisitor);
//
//        return cemt.getComponentDataTypeHashMap();
//    }

    /**
     * Escapes HTML content in strings
     *
     * @param string The string
     * @return The 'escaped' string
     */
    private static String escapeHtml(String string) {
        if (string == null || string.length() == 0) return "";

        StringBuffer sb = new StringBuffer(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        int len = string.length();
        char c;

        for (int i = 0; i < len; i++)
            {
            c = string.charAt(i);
            if (c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you lose
                // word breaking
                if (lastWasBlankChar) {
                    lastWasBlankChar = false;
                    sb.append("&nbsp;");
                    }
                else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                    }
                }
            else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if (c == '"')
                    sb.append("&quot;");
                else if (c == '&')
                    sb.append("&amp;");
                else if (c == '<')
                    sb.append("&lt;");
                else if (c == '>')
                    sb.append("&gt;");
                else if (c == '\n')
                    // Handle Newline
                    sb.append("&lt;br/&gt;");
                else {
                    int ci = 0xffff & c;
                    if (ci < 160 )
                        // nothing special only 7 Bit
                        sb.append(c);
                    else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#");
                        sb.append(new Integer(ci).toString());
                        sb.append(';');
                        }
                    }
                }
            }
        return sb.toString();
    }

    private static void fail(Exception e) {
    	_logger.fatal("", e);
        System.exit(-1);
    }

    /**
     * [Recursively] retrieves file names based on a specified filter
     *
     * @param pathName The start location
     * @param filter The file filter
     * @param recurseSubdir True to recursively search subdirectories, false otherwise
     * @return The list of filenames found
     */
    public static List<String> getFiles(String pathName, FileFilter filter, boolean recurseSubdir) {
        List<String> fileList = new ArrayList<String>();
        getFilesInternal(pathName, filter, fileList, recurseSubdir);

        return fileList;
    }

    private static void getFilesInternal(String pathName, FileFilter filter, List<String> fileList, boolean recurseSubdir) {
        File dir = new File(pathName);
        assert dir.isDirectory();

        for (File file : dir.listFiles()) {
            if (file.isFile() && filter.accept(file))
                fileList.add(file.getAbsolutePath());
            else
                if (file.isDirectory() && recurseSubdir)
                    getFilesInternal(file.getAbsolutePath(), filter, fileList, recurseSubdir);
        }
    }

    /**
     * Specifies that a SOAP protocol error occured
     *
     * @author Boris Capitanu
     *
     */
    private static class SoapClientException extends Exception
    {
		private static final long serialVersionUID = -3973348710770633831L;

		@SuppressWarnings("unused")
        public SoapClientException()
        {
        }

        public SoapClientException(String message)
        {
            super(message);
        }
    }
}
