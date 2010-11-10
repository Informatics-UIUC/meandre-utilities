package org.meandre.tools.components.installer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.meandre.tools.client.AbstractMeandreClient;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

public class InstallComponentsCMD {

    /**
     * holds temporary files created during installation: rdf descriptors, jar
     * files, etc.
     */
    static File _workingDir;

    /**
     * directory of compiled .class files. all component .class files, applet
     * .class files, and supporting classes not in a jar should be in here.
     */
    static File _classDir;

    /**
     * location of external jars the components depend on. this directory will
     * be recursively searched for all jar files.
     */
    static File _jarLibDir;

    /** host name of the running Meandre-Infrastructure instance to install to. */
    static String _serverHost;

    /** port on _serverHost of the running instance */
    static int _serverPort;

    /** the login 'nickname' of the user to log into the server as */
    static String _serverUsername;

    /** the password of user _serverUsername */
    static String _serverPassword;

    public static void main(String[] args) throws Exception {

        // populate this class's static variables with the arg values
        parseArgs(args);

        AbstractMeandreClient mClient = AbstractMeandreClient.getClientForServer(_serverHost, _serverPort, _serverUsername, _serverPassword);
        ComponentInstaller installer = new ComponentInstaller(_workingDir, _classDir, _jarLibDir, mClient);
        installer.installAllComponents();
    }

    /**
     * populate the static variables of this class.
     * 
     * @throws JSAPException
     * @throws UnknownHostException
     */
    private static void parseArgs(String[] args) throws UnknownHostException, JSAPException {

        JSAP jsap = makeCommandLineParser();

        JSAPResult config = jsap.parse(args);

        if (!config.success()) {
            InstallComponentsCMD.exitWithError(config, jsap);
        }

        _workingDir = config.getFile("tmpDir");
        _classDir = config.getFile("classDir");
        _jarLibDir = config.getFile("libDir");
        _serverHost = config.getInetAddress("meandreHost").getHostName();
        _serverPort = config.getInt("meandrePort");
        _serverUsername = config.getString("meandreUsername");
        _serverPassword = config.getString("meandrePassword");
    }

    private static JSAP makeCommandLineParser() throws JSAPException, UnknownHostException {
        JSAP jsap = new JSAP();

        // a parser for arguments that are a file that is a directory
        // that must exist
        FileStringParser requiredDirParser = FileStringParser.getParser();
        requiredDirParser.setMustExist(true);
        requiredDirParser.setMustBeDirectory(true);

        // classesDir for ComponentInstaller
        FlaggedOption classDirOpt = new FlaggedOption("classDir");
        classDirOpt.setShortFlag('c');
        classDirOpt.setLongFlag("class-dir");
        classDirOpt.setStringParser(requiredDirParser);
        classDirOpt.setRequired(true);
        classDirOpt.setHelp("The root directory of the compiled .class files" + " that contains the compiled ExecutableComponent classes. Also any"
                + " supporting classes not in a jar in the lib directory must be" + " here.");
        jsap.registerParameter(classDirOpt);

        // libDir of jars for ComponentInstaller
        FlaggedOption libDirOpt = new FlaggedOption("libDir");
        libDirOpt.setShortFlag('l');
        libDirOpt.setLongFlag("lib-dir");
        libDirOpt.setStringParser(requiredDirParser);
        libDirOpt.setRequired(true);
        libDirOpt.setHelp("The root directory of supporting jar files that " + " one or more components depends on. This directory will be "
                + "recursively scanned for any jar files in it's subdirectories. " + "Jar files declared "
                + "as dependencies in a component's annotations and jars found to " + "contain classes needed by a component from this "
                + "directory will be installed with that component.");
        jsap.registerParameter(libDirOpt);

        // tmpDir for ComponentInstaller
        FlaggedOption tmpDirOpt = new FlaggedOption("tmpDir");
        tmpDirOpt.setShortFlag('t');
        tmpDirOpt.setLongFlag("tmp-dir");
        tmpDirOpt.setStringParser(requiredDirParser);
        tmpDirOpt.setRequired(true);
        tmpDirOpt.setHelp("A directory that temporary and cached files can " + "be written to by the installer. Some intermediate files are "
                + "reusable between runs of InstallComponentsCMD. If they are present "
                + "and the relevant source files are unchanged, they will try to be " + "reused.");
        jsap.registerParameter(tmpDirOpt);

        // hostname of server to install to
        FlaggedOption meandreHostOpt = new FlaggedOption("meandreHost");
        meandreHostOpt.setShortFlag('h');
        meandreHostOpt.setLongFlag("meandre-host");
        meandreHostOpt.setStringParser(JSAP.INETADDRESS_PARSER);
        meandreHostOpt.setRequired(false);
        meandreHostOpt.setDefault(InetAddress.getLocalHost().toString());
        meandreHostOpt.setHelp("The ip address or hostname of the machine" + " running the Meandre-Infrastructure instance being installed on."
                + " Defaults to 'localhost' if unspecified.");
        jsap.registerParameter(meandreHostOpt);

        // port number of server to install to
        FlaggedOption meandrePortOpt = new FlaggedOption("meandrePort");
        meandrePortOpt.setShortFlag('p');
        meandrePortOpt.setLongFlag("meandre-port");
        meandrePortOpt.setStringParser(JSAP.INTEGER_PARSER);
        meandrePortOpt.setRequired(false);
        meandrePortOpt.setDefault("1714");
        meandrePortOpt.setHelp("The port number of the " + " running Meandre-Infrastructure instance being installed on."
                + " Defaults to '1714' if unspecified.");
        jsap.registerParameter(meandrePortOpt);

        // username on installation server
        FlaggedOption meandreUserOpt = new FlaggedOption("meandreUsername");
        meandreUserOpt.setShortFlag('u');
        meandreUserOpt.setLongFlag("meandre-username");
        meandreUserOpt.setStringParser(JSAP.STRING_PARSER);
        meandreUserOpt.setRequired(false);
        meandreUserOpt.setDefault("admin");
        meandreUserOpt.setHelp("Username to log into the Meandre-Infrastructre" + " instance as to perform the upload."
                + " Defaults to 'admin' if unspecified.");
        jsap.registerParameter(meandreUserOpt);

        // password on installation server
        FlaggedOption meandrePasswordOpt = new FlaggedOption("meandrePassword");
        meandrePasswordOpt.setShortFlag('w');
        meandrePasswordOpt.setLongFlag("meandre-password");
        meandrePasswordOpt.setStringParser(JSAP.STRING_PARSER);
        meandrePasswordOpt.setRequired(false);
        meandrePasswordOpt.setDefault("admin");
        meandrePasswordOpt.setHelp("Password of Username for logging into the" + " Meandre-Infrastructre instance."
                + " Defaults to 'admin' if unspecified.");
        jsap.registerParameter(meandrePasswordOpt);

        return jsap;
    }

    /**
     * error message when the result of the parse fails. The contents of this
     * method are copied from the JASP tutorial at:
     * http://www.martiansoftware.com/jsap/doc/
     * 
     * @param parseResult
     *            the jsapResult returned when the commandline args where
     *            parsed. assumes this 'has errors'
     * @param jsap
     *            the jsap used to parse the commandline args that created the
     *            error result parseResult
     */
    private static void exitWithError(JSAPResult parseResult, JSAP jsap) {
        System.err.println();

        for (java.util.Iterator<?> errs = parseResult.getErrorMessageIterator(); errs.hasNext();) {
            System.err.println("Error: " + errs.next());
        }

        System.err.println();
        System.err.println("Usage: java " + InstallComponentsCMD.class.getName());
        System.err.println("                " + jsap.getUsage());
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(1);
    }

}
