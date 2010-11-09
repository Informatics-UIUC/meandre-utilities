package org.meandre.tools.repository;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.seasr.meandre.support.generic.io.ClientHttpRequest;
import org.seasr.meandre.support.generic.io.IOUtils;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.defaultsources.PropertyDefaultSource;

import de.schlichtherle.io.FileReader;

public class LocationToSC {

    private static final Logger _logger = Logger.getLogger("LocationToSC");

    private static final String SC_COMP_UPLOAD = "/services/user/.../upload/component.json";
    private static final String SC_FLOW_UPLOAD = "/services/user/.../upload/flow.json";

    private static String _locationBase;

    private static boolean _verboseOutput;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // Parse command line arguments
        JSAPResult jsapResult = parseArguments(args);

        // Extract the argument values
        String location = jsapResult.getString("location");
        String server = jsapResult.getString("server");
        int port = jsapResult.getInt("port");
        String user = jsapResult.getString("user");
        //String password = jsapResult.getString("password");
        String[] flows = jsapResult.getStringArray("flow");
        String flowsFile = jsapResult.getString("flow file");
        boolean bAllFlows = jsapResult.getBoolean("all flows");
        String[] components = jsapResult.getStringArray("component");
        String compFile = jsapResult.getString("component file");
        boolean bAllComponents = jsapResult.getBoolean("all components");
        _verboseOutput = jsapResult.getBoolean("verbose");

        _locationBase = location.substring(0, location.lastIndexOf("/"));

        if (flowsFile != null && flows.length == 0)
            flows = readFile(flowsFile);
        else
            if (flowsFile != null && flows.length > 0) {
                System.err.println("Options -f and --flowFile cannot be used together");
                return;
            }

        if (compFile != null && components.length == 0)
            components = readFile(compFile);
        else
            if (compFile != null && components.length > 0) {
                System.err.println("Options -c and --compFile cannot be used together");
                return;
            }

        QueryableRepository qr = getRepository(location);

        Map<String, ExecutableComponentDescription> componentsMap = qr.getAvailableExecutableComponentDescriptionsMap();

        if (components.length == 0 && bAllComponents) {
            components = new String[componentsMap.size()];
            componentsMap.keySet().toArray(components);
        }

        URL scCompUpload = new URL("http", server, port, SC_COMP_UPLOAD.replaceFirst("\\.\\.\\.", user));
        for (String compUri : components) {
            ExecutableComponentDescription ecd = componentsMap.get(compUri);
            if (_verboseOutput)
                System.out.println(String.format("%nUploading component: %s", ecd.getName()));

            uploadComponent(ecd, scCompUpload);
        }

        Map<String, FlowDescription> flowsMap = qr.getAvailableFlowDescriptionsMap();

        if (flows.length == 0 && bAllFlows) {
            flows = new String[flowsMap.size()];
            flowsMap.keySet().toArray(flows);
        }

        URL scFlowUpload = new URL("http", server, port, SC_FLOW_UPLOAD.replaceFirst("\\.\\.\\.", user));
        for (String flowUri : flows) {
            FlowDescription fd = flowsMap.get(flowUri);
            if (_verboseOutput)
                System.out.println(String.format("%nUploading flow: %s", fd.getName()));

            uploadFlow(fd, scFlowUpload);
        }

        if (components.length == 0 && flows.length == 0)
            System.err.println("Nothing to do. Please specify some components and/or flows to upload!");
    }

    private static void uploadFlow(FlowDescription fd, URL scFlowUpload) throws Exception {
        ClientHttpRequest req = new ClientHttpRequest(scFlowUpload);
        req.setParameter("flow_rdf", "model.rdf", ModelUtils.getInputStreamForModel(fd.getModel(), "RDF/XML"));

        InputStream response = req.post();

        JSONObject result = new JSONObject(IOUtils.getTextFromReader(new InputStreamReader(response)));
        System.out.print(String.format("%s \t--> ", fd.getName()));

        if (result.has("error_msg"))
            System.err.println(String.format("Error: %s", result.getString("error_msg")));
        else
            System.out.println(result.getString("url"));
    }

    private static void uploadComponent(ExecutableComponentDescription ecd, URL scCompUpload) throws Exception {
        int nContext = 0;

        ClientHttpRequest req = new ClientHttpRequest(scCompUpload);
        req.setParameter("component_rdf", "model.rdf", ModelUtils.getInputStreamForModel(ecd.getModel(), "RDF/XML"));

        // Iterate through all the component context files
        Set<RDFNode> compContext = ecd.getContext();
        for (RDFNode context : compContext) {
            String url = context.toString();

            if (context.isResource() && !url.toLowerCase().endsWith("/")) {
                URI contextURI = new URI(url);
                if (contextURI.getScheme().equals("context"))
                    contextURI = new URI(_locationBase + contextURI.getPath());
                if (_verboseOutput)
                    System.out.println(String.format("\t%s", contextURI));
                String ctxFileName = url.substring(url.lastIndexOf("/") + 1);
                req.setParameter("context_" + nContext++, ctxFileName, contextURI.toURL().openStream());
            }
        }

        InputStream response = req.post();

        JSONObject result = new JSONObject(IOUtils.getTextFromReader(new InputStreamReader(response)));
        System.out.print(String.format("%s \t--> ", ecd.getName()));

        if (result.has("error_msg"))
            System.err.println(String.format("Error: %s", result.getString("error_msg")));
        else
            System.out.println(result.getString("url"));
    }

    private static String[] readFile(String fileName) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = null;
        Set<String> setLines = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0)
                setLines.add(line);
        }

        String[] lines = new String[setLines.size()];
        setLines.toArray(lines);

        return lines;
    }

    private static QueryableRepository getRepository(String location) throws MalformedURLException, IOException {
        URL locationURL = new URL(location);
        Model model = ModelUtils.getModel(locationURL.openStream(), null);
        return new RepositoryImpl(model);
    }

    /**
     * Parses the command line arguments
     *
     * @param args The command line arguments
     * @return The JSAPResult object containing the parsed arguments
     */
    private static JSAPResult parseArguments(String[] args) {
        JSAPResult result = null;

        String generalHelp = "Loads components and/or flows from a remote repository into a SEASR Central server";

        try {
            PropertyDefaultSource defaultSource = new PropertyDefaultSource(
                    System.getProperty("user.home") + "/.SEASR/LocationToSC.conf", false);

            SimpleJSAP jsap =
                    new SimpleJSAP("LocationToSC",
                                   generalHelp,
                                   new Parameter[]{
                                           new FlaggedOption("location", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 'l',
                                                             "location", "The location to read from"),
                                           new FlaggedOption("server", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 's',
                                                             "server", "SEASR Central server host name"),
                                           new FlaggedOption("port", JSAP.INTEGER_PARSER,
                                                             "8090", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "port", "SEASR Central server port number"),
                                           new FlaggedOption("user", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u',
                                                             "user", "User name under which the upload will take place"),
                                           //new FlaggedOption("password", JSAP.STRING_PARSER,
                                           //                  JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p',
                                           //                  "password", "User password"),
                                           new FlaggedOption("flow", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f',
                                                             "flow", "The flow name; repeat as necessary")
                                                   .setAllowMultipleDeclarations(true),
                                           new FlaggedOption("flow file", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "flowFile", "The file to read the flow names from"),
                                           new Switch("all flows", JSAP.NO_SHORTFLAG, "all-flows", "Upload all flows"),
                                           new FlaggedOption("component", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'c',
                                                             "component", "The component name; repeat as necessary")
                                                   .setAllowMultipleDeclarations(true),
                                           new FlaggedOption("component file", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "compFile", "The file to read the component names from"),
                                           new Switch("all components", JSAP.NO_SHORTFLAG, "all-components", "Upload all components"),
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

    private static void fail(Exception e) {
        _logger.log(Level.SEVERE, "Aborting execution", e);
        //e.printStackTrace();
        System.exit(-1);
    }
}
