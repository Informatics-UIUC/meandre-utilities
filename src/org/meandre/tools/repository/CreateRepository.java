package org.meandre.tools.repository;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.ExecutableComponentInstanceDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.meandre.tools.client.AbstractMeandreClient;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.seasr.meandre.support.generic.io.IOUtils;
import org.seasr.meandre.support.generic.io.webdav.WebdavClient;
import org.seasr.meandre.support.generic.io.webdav.WebdavClientFactory;
import org.seasr.meandre.support.generic.util.KeyValuePair;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.defaultsources.PropertyDefaultSource;

/**
 * Tool that allows the user to create a remote repository based on a flow, a set of flows,
 * or based on the entire repository identified by 'server' in the command line parameters
 * <p/>
 * Run with --help as argument to see the help menu
 *
 * @author Boris Capitanu
 */
public class CreateRepository {
    private static final Logger _logger = Logger.getLogger("CreateRepository");

    private static String JARTOOL_BASE_URL;

    private static boolean _verboseOutput;
    private static boolean _skipUpload;
    private static boolean _makeRelative;

    public static void main(String[] args) throws Exception {
        // Parse command line arguments
        JSAPResult jsapResult = parseArguments(args);

        // Extract the argument values
        String server = jsapResult.getString("server");
        int port = jsapResult.getInt("port");
        String user = jsapResult.getString("user");
        String password = jsapResult.getString("password");
        String destination = jsapResult.getString("destination");
        String[] flows = jsapResult.getStringArray("flow");
        String flowsFile = jsapResult.getString("flow file");
        String[] components = jsapResult.getStringArray("component");
        String compFile = jsapResult.getString("component file");
        String davUser = jsapResult.getString("webdav username");
        String davPassword = jsapResult.getString("webdav password");
        _verboseOutput = jsapResult.getBoolean("verbose");
        _skipUpload = jsapResult.getBoolean("skip-upload");
        _makeRelative =jsapResult.getBoolean("make-relative");

        JARTOOL_BASE_URL = "http://" + server + ":" + port + "/plugins/jar/";

        Credentials davCredentials = (davUser != null) ?
                new UsernamePasswordCredentials(davUser, (davPassword != null) ? davPassword : "") : null;

        if (flows.length > 0 && flowsFile != null) {
            System.err.println("Options -f and --flowFile cannot be used together");
            return;
        }

        if (components.length > 0 && compFile != null) {
            System.err.println("Options -c and --compFile cannot be used together");
            return;
        }

        if (flows.length == 0 && flowsFile != null) {
            try {
                System.out.println("Getting list of flows...");
                BufferedReader reader = new BufferedReader(new FileReader(flowsFile));
                String flowName = null;
                Set<String> setFlows = new HashSet<String>();
                while ((flowName = reader.readLine()) != null) {
                    flowName = flowName.trim();
                    if (flowName.length() > 0)
                        setFlows.add(flowName);
                }

                flows = new String[setFlows.size()];
                setFlows.toArray(flows);
            }
            catch (Exception e) {
                fail(e);
            }
        }

        if (components.length == 0 && compFile != null) {
            try {
                System.out.println("Getting list of components...");
                BufferedReader reader = new BufferedReader(new FileReader(compFile));
                String compName = null;
                Set<String> setComponents = new HashSet<String>();
                while ((compName = reader.readLine()) != null) {
                    compName = compName.trim();
                    if (compName.length() > 0)
                        setComponents.add(compName);
                }

                components = new String[setComponents.size()];
                setComponents.toArray(components);
            }
            catch (Exception e) {
                fail(e);
            }
        }

        // Setting logger levels
        _logger.setLevel(Level.WARNING);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "error");

        AbstractMeandreClient meandre = AbstractMeandreClient.getClientForServer(server, port, user, password);
        meandre.setLogger(_logger);

        System.out.println(String.format("%nQuerying repository: http://%s:%s%n", server, port));

        // Create the repository model (that stores the flows and/or components)
        Model repositoryModel = null;

        try {
            repositoryModel = retrieveModel(meandre, flows, components);
        } catch (TransmissionException e) {
            fail(e);
        }

        // Create a mapping between old context URIs and new context URIs
        Map<String, String> contextMap = relocateContext(destination, repositoryModel, String.format("http://%s:%s", server, port));

        // Create the remote repository and upload the context files
        createRemoteRepository(destination, davCredentials, repositoryModel, contextMap);
    }

    /**
     * Creates the remote repository containing all the flows, components, and their context
     *
     * @param destination     The WebDAV destination url for the repository
     * @param repositoryModel The model containing the flows/components descriptors
     * @param contextMap      The relocation information for component contexts
     * @return The url of the new repository
     * @throws IOException   Thrown if a I/O error occurs
     * @throws HttpException Thrown if a problem occurs when uploading the context to the new repository
     * @throws URISyntaxException
     */
    private static void createRemoteRepository(String destination, Credentials davCredentials,
                                                 Model repositoryModel, Map<String, String> contextMap)
            throws IOException, URISyntaxException {

        // @jira TOOLS-1
        Model[] models = splitModel(repositoryModel);
        Model componentModel = models[0];
        Model flowModel = models[1];

        HttpHost davHost = new HttpHost(destination);                
        WebdavClient webdav = WebdavClientFactory.begin(davHost, davCredentials);

        if (!webdav.mkdirs(destination))
            throw new IOException("Cannot create directory: " + destination);

        System.out.println();

        if (!_skipUpload) {
            // Upload the component context data to WebDAV
            for (String context : contextMap.keySet()) {
                String newContextUri = contextMap.get(context);
                URL contextURL = new URL(context);

                // Create the necessary webdav folders
                String sDirectory = newContextUri.substring(0, newContextUri.lastIndexOf("/"));
                if (webdav.mkdirs(sDirectory)) {

                    boolean skipFile = false;

                    String contextFileName = contextURL.getPath();
                    contextFileName = contextFileName.substring(contextFileName.lastIndexOf("/") + 1);

                    String serverMD5 = getMD5ForResource(contextFileName);

                    // check if the context file already exists and is the same
                    String md5Uri = newContextUri + ".md5";
                    try {
                        String remoteMD5 = IOUtils.getTextFromReader(IOUtils.getReaderForResource(new URI(md5Uri))).replaceAll("\\r|\\n", "");
                        skipFile = remoteMD5.equalsIgnoreCase(serverMD5);
                    }
                    catch (FileNotFoundException e) {
                    }

                    if (!skipFile) {
                        // ... and upload the context file
                        System.out.println("  Uploading: " + newContextUri);
                        //System.out.println("       From: " + contextURL);

                        webdav.put(newContextUri, contextURL.openStream());
                        webdav.put(md5Uri, serverMD5.getBytes());
                    } else
                        System.out.println("   Skipping: " + newContextUri);
                }
                else
                    _logger.log(Level.SEVERE, "execute: Could not create all subpaths for " + sDirectory);
            }

            System.out.println();
        }

        @SuppressWarnings("unchecked")
        KeyValuePair<String,String>[] formats = new KeyValuePair[] {
                new KeyValuePair<String, String>("TTL", ".ttl"),
                new KeyValuePair<String, String>("RDF/XML", ".rdf"),
                new KeyValuePair<String, String>("N-TRIPLE", ".nt")
        };

        String repositoryUri = destination + "/repository";

        if (componentModel.size() > 0 && flowModel.size() > 0) {
            for (KeyValuePair<String,String> format : formats) {
                    // Get the repository location
                    String repoUri = repositoryUri + format.getValue();
                    System.out.println("Creating master repository: " + repoUri);

                    // Save the repository model
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    repositoryModel.write(baos, format.getKey());
                    webdav.put(repoUri, baos.toByteArray());
            }

            System.out.println();
        }

        if (componentModel.size() > 0) {
            for (KeyValuePair<String,String> format : formats) {
                // @jira TOOLS-1
                String compRepoUri = repositoryUri + "_components" + format.getValue();
                System.out.println("Creating components repository: " + compRepoUri);

                // Save the component model
                ByteArrayOutputStream compBaos = new ByteArrayOutputStream();
                componentModel.write(compBaos, format.getKey());
                webdav.put(compRepoUri, compBaos.toByteArray());
            }

            System.out.println();
        }

        if (flowModel.size() > 0) {
            for (KeyValuePair<String,String> format : formats) {
                String flowRepoUri = repositoryUri + "_flows" + format.getValue();
                System.out.println("Creating flows repository: " + flowRepoUri);

                // Save the flow model
                ByteArrayOutputStream flowBaos = new ByteArrayOutputStream();
                flowModel.write(flowBaos, format.getKey());
                webdav.put(flowRepoUri, flowBaos.toByteArray());
            }

            System.out.println();
        }
    }

    /**
     * Splits a model containing both flows and components into two separate ones
     * containing just flows, and just components, respectively.
     *
     * @param repositoryModel The combined model
     * @return An array where the fist element is the component model, and the second the flow model
     */
    private static Model[] splitModel(Model repositoryModel) {
        RepositoryImpl repos = new RepositoryImpl(repositoryModel);

        Model compModel = ModelFactory.createDefaultModel();
        for (ExecutableComponentDescription ecd : repos.getAvailableExecutableComponentDescriptions())
            compModel.add(ecd.getModel());

        Model flowModel = ModelFactory.createDefaultModel();
        for (FlowDescription fd : repos.getAvailableFlowDescriptions())
            flowModel.add(fd.getModel());

        return new Model[] { compModel, flowModel };
    }

    /**
     * Relocates all the component context information in the model to the new destination
     *
     * @param destination     The destination WebDAV location url
     * @param repositoryModel The model
     * @return A mapping between old context locations and new context locations
     */
    private static Map<String, String> relocateContext(String destination, Model repositoryModel, String server) {
        RepositoryImpl repository = new RepositoryImpl(repositoryModel);
        Map<String, String> contextMap = new HashMap<String, String>();

        // Get all the component descriptions
        Set<ExecutableComponentDescription> componentDescriptions =
                repository.getAvailableExecutableComponentDescriptions();

        // Go through each component
        for (ExecutableComponentDescription compDesc : componentDescriptions) {
            // TODO: something is wrong with Python components, they give out 'null'
            if (compDesc == null) continue;

            if (_verboseOutput)
                System.out.println("   Component: " + compDesc.getName());

            // Holder for component dependencies
            StringBuilder dependencies = new StringBuilder();

            // Iterate through all the component context files
            Set<RDFNode> compContext = compDesc.getContext();
            for (RDFNode context : compContext) {
                String contextUrl = context.toString();

                // rewrite the context URL if it points to localhost
                if (contextUrl.startsWith("http://127.0.0.1") || contextUrl.startsWith("http://localhost")) {
                    try {
                        URL url = new URL(contextUrl);
                        contextUrl =  server + url.getPath();
                    }
                    catch (MalformedURLException e) {
                        fail(e);
                    }
                }

                if (context.isResource() && contextUrl.toLowerCase().endsWith(".jar")) {
                    // Generate the new location for the context
                    String contextName = contextUrl.substring(contextUrl.lastIndexOf('/') + 1);
                    String newContextURL = destination + "/resources/" + contextName;
                    Resource newContextName = repositoryModel.createResource(
                            _makeRelative ? "context://localhost/resources/" + contextName : newContextURL);

                    if (!newContextURL.equals(contextUrl)) {
                        // ... and save the mapping between old and new context location
                        contextMap.put(contextUrl, newContextURL);

                        if (_verboseOutput)
                            System.out.println("   Relocating " + contextName + " to " + newContextURL);

                        // ... now update the model with the new location
                        repositoryModel.remove(compDesc.getExecutableComponent(),
                                RepositoryVocabulary.execution_context, context);
                        repositoryModel.add(compDesc.getExecutableComponent(),
                                RepositoryVocabulary.execution_context, newContextName);
                    } else
                        System.out.println("    Warning: Resource " + contextName + " is already referenced from this location, skipping relocation.");

                    if (dependencies.length() > 0)
                        dependencies.append(", ");

                    dependencies.append(contextName);
                }
            }

            if (_verboseOutput) {
                System.out.println("Dependencies: " + dependencies.toString());
                System.out.println("");
            }
        }

        return contextMap;
    }

    /**
     * Creates a JENA model that stores all the flows (and associated components) specified
     *
     * @param server The Meandre server where the repository to be queried resides
     * @param flows  The array of flow URIs for the flows to retrieve (or empty array to retrieve all flows)
     * @return The model
     * @throws TransmissionException
     */
    private static Model retrieveModel(AbstractMeandreClient meandre, String[] flows, String[] components)
            throws TransmissionException {
        Model repositoryModel;

        if (flows.length == 0 && components.length == 0)
            repositoryModel = meandre.retrieveRepository().getModel();
        else {
            Set<String> componentURIs = new HashSet<String>();
            repositoryModel = ModelFactory.createDefaultModel();

            // Go through all the flows the user specified
            for (String flowUri : flows) {
                System.out.println("Processing flow: " + flowUri);

                // Get the flow descriptor
                FlowDescription flowDescriptor = meandre.retrieveFlowDescriptor(flowUri);

                // Get the flow RDF and create a Jena model from it
                Model flowModel = flowDescriptor.getModel();

                // Add the flow model to the repository model
                repositoryModel.add(flowModel);

                // Get the set of component resources used in the flow
                for (ExecutableComponentInstanceDescription ecid : flowDescriptor.getExecutableComponentInstances())
                    componentURIs.add(ecid.getExecutableComponent().getURI());
            }

            for (String compUri : components) {
                // skip the components that have already been added
                if (componentURIs.contains(compUri))
                    continue;

                componentURIs.add(compUri);
            }

            // Add each component model to the repository model
            for (String componentURI : componentURIs) {
                System.out.println("Processing component: " + componentURI);
                Model componentModel = meandre.retrieveComponentDescriptor(componentURI).getModel();
                repositoryModel.add(componentModel);
            }
        }

        return repositoryModel;
    }

    /**
     * Parses the command line arguments
     *
     * @param args The command line arguments
     * @return The JSAPResult object containing the parsed arguments
     */
    private static JSAPResult parseArguments(String[] args) {
        JSAPResult result = null;

        String generalHelp = "Creates/updates a remote WebDAV repository for the specified flow(s) " +
                             "and/or component(s) and returns the location to it";

        try {
            PropertyDefaultSource defaultSource = new PropertyDefaultSource(
                    System.getProperty("user.home") + "/.SEASR/CreateRepository.conf", false);

            SimpleJSAP jsap =
                    new SimpleJSAP("CreateRepository",
                                   generalHelp,
                                   new Parameter[]{
                                           new FlaggedOption("server", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 's',
                                                             "server", "Meandre server name"),
                                           new FlaggedOption("port", JSAP.INTEGER_PARSER,
                                                             "1714", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "port", "Meandre server port"),
                                           new FlaggedOption("user", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u',
                                                             "user", "Meandre user name"),
                                           new FlaggedOption("password", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p',
                                                             "password", "Meandre user password"),
                                           new FlaggedOption("flow", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f',
                                                             "flow", "The flow name; repeat as necessary")
                                                   .setAllowMultipleDeclarations(true),
                                           new FlaggedOption("flow file", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "flowFile", "The file to read the flow names from"),
                                           new FlaggedOption("component", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'c',
                                                             "component", "The component name; repeat as necessary")
                                                   .setAllowMultipleDeclarations(true),
                                           new FlaggedOption("component file", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "compFile", "The file to read the component names from"),
                                           new FlaggedOption("destination", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd',
                                                             "destination", "The destination where to create the " +
                                                                            "repository"),
                                           new FlaggedOption("webdav username", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "davuser", "The destination WebDAV user name"),
                                           new FlaggedOption("webdav password", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "davpassword", "The destination WebDAV password"),
                                           new Switch("skip-upload", JSAP.NO_SHORTFLAG, "skip-upload", "Skip uploading of JAR dependencies"),
                                           new Switch("make-relative", 'r', "make-relative", "Generate relative context locations"),
                                           new Switch("verbose", 'v', "verbose", "Enable verbose output")});

            jsap.registerDefaultSource(defaultSource);
            result = jsap.parse(args);
            if (jsap.messagePrinted())
                System.exit(-1);
        }
        catch (JSAPException e) {
            fail(e);
        }

        return result;
    }

    public static String getMD5ForResource(String sFile) {
        try {
            URL req = new URL(JARTOOL_BASE_URL + sFile + "/md5");
            String resp = IOUtils.getTextFromReader(IOUtils.getReaderForResource(req.toURI()));

            return new JSONObject(resp).getString("md5");
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        catch (FileNotFoundException e) {
            return null;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    private static void fail(Exception e) {
        _logger.log(Level.SEVERE, "Aborting execution", e);
        //e.printStackTrace();
        System.exit(-1);
    }
}
