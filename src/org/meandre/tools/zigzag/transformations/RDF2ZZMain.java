/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.meandre.tools.zigzag.transformations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.tools.client.AbstractMeandreClient;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.defaultsources.PropertyDefaultSource;

/**
 * Entry point for the rdf2zz application
 * Invoke with argument "--help" to obtain the usage information
 *
 * Dependencies:
 * 
 * jsap-2.1.jar
 *
 * jena-2.6.3.jar
 * slf4j-api-1.5.8.jar
 * slf4j-log4j12-1.5.8.jar
 * log4j-1.2.13.jar
 * icu4j-3.4.4.jar
 * iri-0.8.jar
 * xercesImpl-2.7.1.jar
 * arq-2.8.4.jar
 * lucene-core-2.3.1.jar
 * 
 * commons-codec-1.3.jar
 * commons-logging-1.1.1.jar
 * httpclient-4.0.3.jar
 * httpcore-4.0.1.jar
 * httpmime-4.0.3.jar
 * apache-mime4j-0.6.jar
 * 
 * json.jar
 * 
 * meandre-client-1.4.9.jar
 * meandre-kernel-1.4.9.jar
 * seasr-commons.jar
 *
 * @author Boris Capitanu
 *
 */
public class RDF2ZZMain {
    private final static Logger _logger = Logger.getLogger(RDF2ZZMain.class.getSimpleName());

    /**
     * @param args Cmd line arguments
     * @throws IOException Thrown when a I/O error occurs
     * @throws TransmissionException Thrown when there is a problem communicating with the Meandre server
     * @throws FlowNotFoundException Thrown when the specified flow does not exist
     */
    public static void main(String[] args) throws IOException, TransmissionException, FlowNotFoundException {
        // configure apache logging
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "error");

        QueryableRepository repository = null;
        String importSource;

        _logger.setLevel(Level.SEVERE);

        JSAPResult jsapResult = parseArguments(args);
        String inputRDFFileURL = jsapResult.getString("input rdf");

        if (inputRDFFileURL != null && inputRDFFileURL.length() > 0) {
            URL inputURL = null;
            try {
                inputURL = new URL(inputRDFFileURL);
            } 
            catch (Exception e) {
                File file = new File(inputRDFFileURL);
                inputURL = file.toURI().toURL();
            }
            
            Model model = ModelUtils.getModel(inputURL.openStream(), null);
            repository = new RepositoryImpl(model);
            importSource = inputURL.toString();
        } else {
            String server = jsapResult.getString("server");
            int port = jsapResult.getInt("port");
            String user = jsapResult.getString("user");
            String password = jsapResult.getString("password");

            if (server == null)
                parseArguments(new String[] { "--help" });

            AbstractMeandreClient client = AbstractMeandreClient.getClientForServer(server, port, user, password);
            client.setLogger(_logger);
            if (user != null && password != null) {
                repository = client.retrieveRepository();
                importSource = null;
            } else {
                repository = client.retrievePublicRepository();
                importSource = new URL("http", server, port, "/public/services/repository.ttl").toString();
            }
        }

        String flowURI = jsapResult.getString("flow");
        if (flowURI == null) {
            System.out.println("No flow URI specified. Available flow URIs:");
            for (FlowDescription flow : repository.getAvailableFlowDescriptions())
                System.out.println(String.format("\t%s  (%s)", flow.getFlowComponentAsString(), flow.getName()));
            System.exit(1);
        }

        RDF2ZZConverter converter = new RDF2ZZConverter(repository);
        converter.setImportSource(importSource);
        String zzScript = converter.generateZZ(flowURI);

        OutputStream outStream = System.out;
        String outputFileName = jsapResult.getString("output zz");

        if (outputFileName != null)
            outStream = new FileOutputStream(outputFileName);

        outStream.write(zzScript.getBytes());
        outStream.close();
    }

    private static JSAPResult parseArguments(String[] args) {
        JSAPResult result = null;

        String generalHelp = "Converts a flow into a ZigZag script";

        try {
            PropertyDefaultSource defaultSource = new PropertyDefaultSource(
                    System.getProperty("user.home") + "/.SEASR/rdf2zz.conf", false);

            SimpleJSAP jsap =
                    new SimpleJSAP("rdf2zz",
                                   generalHelp,
                                   new Parameter[]{
                                           new FlaggedOption("server", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 's',
                                                             "server", "Meandre server"),
                                           new FlaggedOption("port", JSAP.INTEGER_PARSER,
                                                             "1714", JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,
                                                             "port", "Meandre port"),
                                           new FlaggedOption("user", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'u',
                                                             "user", "Meandre user name"),
                                           new FlaggedOption("password", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'p',
                                                             "password", "Meandre user password"),
                                           new FlaggedOption("flow", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f',
                                                             "flow", "The flow URI"),
                                           new FlaggedOption("output zz", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o',
                                                             "output", "The output ZZ file name"),
                                           new FlaggedOption("input rdf", JSAP.STRING_PARSER,
                                                             JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'i',
                                                             "input", "The file name or URL of the RDF file to be converted")
                                          });

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
        fail(null, e);
    }

    private static void fail(String msg, Exception e) {
        if (msg == null) msg = "Aborting execution";

        _logger.log(Level.SEVERE, msg, e);

        System.exit(-1);
    }
}
