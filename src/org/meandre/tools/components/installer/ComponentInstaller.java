package org.meandre.tools.components.installer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.meandre.core.repository.CorruptedDescriptionException;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.tools.client.AbstractMeandreClient;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.meandre.tools.components.installer.util.CachingDependencyAnalyzer;
import org.meandre.tools.components.installer.util.DependencyAnalyzer;
import org.meandre.tools.components.installer.util.FileTreeIterator;
import org.meandre.tools.components.installer.util.FileUtil;
import org.meandre.tools.components.installer.util.SourceUtil;
import org.seasr.meandre.support.generic.io.FileUtils;

/**
 * ComponentInstaller performs all necessary steps to upload a working Meandre
 * Component to a running Meandre Infrastructure Server starting from a compiled
 * class file (with annotations). This includes discovering and uploading
 * external jar libraries, creating and uploading rdf descriptors, bundling
 * .class files into jars, special considerations for applets, etc.
 * 
 * @author pgroves
 */

public class ComponentInstaller {

    DependencyAnalyzer _depFinder;

    File _jarLibDir;

    File _classDir;

    File _workingDir;

    File _rdfOutputDir;
    File _jarOutputDir;

    AbstractMeandreClient _mClient;
    
    final URLClassLoader loader;

    ComponentJarBuilder _compJarBuilder;
    AppletJarBuilder _appletJarBuilder;
    
    static boolean _verbose = false;

    /**
     * creates an installer ready to analyze dependencies, create rdf
     * descriptors, and upload to a MeandreServer.
     * 
     * @param workingDir
     *            a directory to write temporary files to. this will not be
     *            cleaned up when done... the contents may be reused by later
     *            installation runs if the temp files are not stale in
     *            comparison to the source files.
     * 
     * @param classesDir
     *            root directory of the package tree of compiled .class files.
     *            only components with class files in this directory can be
     *            installed. any applets or other required classes (not found in
     *            a jar in jarLibDir) must also be present in this directory.
     * 
     * @param jarLibDir
     *            root directory containing any external jar file dependencies
     *            the components may contain. jar files in this directory that
     *            are dependended on by components will be uploaded with those
     *            components.
     * 
     * @param uploadClient
     *            the MeandreClient that will be uploaded to. it must have it's
     *            credentials already set and be able to access a running server
     * 
     * @throws IOException
     *             if problem initializing a dependency analyzer in the
     *             classesDir and jarLibDir.
     */
    public ComponentInstaller(File workingDir, File classesDir, File jarLibDir, AbstractMeandreClient uploadClient, boolean verbose) throws IOException {

        _verbose = verbose;
        
        _depFinder = new CachingDependencyAnalyzer(jarLibDir, classesDir, workingDir);
        _jarLibDir = jarLibDir;
        _classDir = classesDir;

        _mClient = uploadClient;

        _workingDir = workingDir;
        _jarOutputDir = new File(_workingDir, "component-jars");
        _jarOutputDir.mkdirs();

        // setupWorkingDir();
        _compJarBuilder = new ComponentJarBuilder(_classDir, _jarOutputDir, _depFinder);
        _appletJarBuilder = new AppletJarBuilder(_classDir, _jarOutputDir, _depFinder);
        
        List<File> classPath = new ArrayList<File>();
        classPath.add(classesDir);
        FileUtils.findFiles(jarLibDir, new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") || name.endsWith(".JAR");
            }
        }, true, classPath);
        
        URL[] classUrls = new URL[classPath.size()];
        for (int i = 0, iMax = classPath.size(); i < iMax; i++)
            classUrls[i] = classPath.get(i).toURI().toURL();
        
        loader = new URLClassLoader(classUrls);
    }

    public void installComponent(String componentClassName) throws IOException, ClassNotFoundException, CorruptedDescriptionException,
            TransmissionException {

        File compClassFile = SourceUtil.classNameToClassFile(componentClassName, _classDir);

        // make rdf descriptor, write it to file
        //
        logInfo("\tReading Annotations");
        Class<?> componentKlass = SourceUtil.classNameToClass(componentClassName, loader);
        ComponentSourceDescriptor compDescriptor = new ComponentSourceDescriptor(componentKlass);
        // for now, don't write the file out as we aren't doing caching yet
        // TODO: write the rdf to file to cache between installer runs.
        // File rdfFile = compDescriptor.writeRDFToDir(_rdfOutputDir);

        // start aggregating the jar files to upload for this component to work
        Set<File> jarFilesToUpload = new HashSet<File>();

        logInfo("\tBuilding Component Jar File");
        // make jar file of component's .class file and any class dependencies
        ComponentJarBuilder compJarBuilder = new ComponentJarBuilder(_classDir, _jarOutputDir, _depFinder);
        File compJarFile = compJarBuilder.writeJar(compDescriptor);
        jarFilesToUpload.add(compJarFile);

        logInfo("\tLooking up Dependencies");
        // find it's jar dependencies, including dependencies of other .class
        // files it needs
        Set<File> classDeps = _depFinder.getDeepClassDeps(compClassFile);
        Set<File> libJarDeps = _depFinder.getDeepJarDeps(classDeps);
        // Remove any JARs that are part of the Meandre server (and shouldn't be uploaded)
        for (File jarFile : libJarDeps) {
            String jarName = jarFile.getName().toLowerCase();
            if (jarName.startsWith("meandre-")) continue;
            jarFilesToUpload.add(jarFile);
        }
        
        // include any jars specified explicitly in the component annotations
        // that may not have been picked up by the DependencyAnalyzer
        Set<String> jarFileBaseNames = compDescriptor.getDeclaredJarDependencies();
        Set<File> explicitJarFiles = FileUtil.findFilesInDirectory(jarFileBaseNames, _jarLibDir);
        jarFilesToUpload.addAll(explicitJarFiles);

        // handle applet uploading, if necessary
        if (compDescriptor.hasApplet()) {
            Set<File> appletJarFiles = getAllAppletJarDependencies(compDescriptor);
            jarFilesToUpload.addAll(appletJarFiles);
        }
        
        logInfo("\tUploading Model and Jars");
        // always overwrite any existing component with this one
        boolean bOverwrite = true;
        
        StringBuilder sb = new StringBuilder();
        for (File f : jarFilesToUpload)
            sb.append(", ").append(f.getName());
        System.out.println(String.format("Installing: %s\t(%s)", componentClassName, sb.substring(2)));
        
        ExecutableComponentDescription ecd = compDescriptor.toExecutableComponentDescription();
        _mClient.uploadComponent(ecd, jarFilesToUpload, bOverwrite);
    }

    /**
     * just does a 'removeResource' on the meandre server for the url
     * representing the component. This is just here as a convenience to have an
     * inverse operation of installComponent with a consistent syntax.
     * 
     * @throws URISyntaxException
     */
    public void uninstallComponent(String componentClassName) throws IOException, ClassNotFoundException, CorruptedDescriptionException,
            TransmissionException, URISyntaxException {

        Class<?> componentKlass = SourceUtil.classNameToClass(componentClassName, loader);
        ComponentSourceDescriptor compDescriptor = new ComponentSourceDescriptor(componentKlass);
        // to delete from a server, we just need to pass the url identifier
        URI compUrl = compDescriptor.getURI();
        _mClient.removeResource(compUrl.toString());
    }

    /**
     * finds all jars necessary for an applet from a component to run. this
     * includes making a jar file with the applet's class file and any
     * dependencies in the classes directory.
     * 
     * @throws IOException
     *             if problem resolving class or jar dependencies
     */
    private Set<File> getAllAppletJarDependencies(ComponentSourceDescriptor compDescriptor) throws IOException {

        // accumulate all jars needed in this set
        Set<File> appletJars = new HashSet<File>();

        Set<String> appletClassNames = compDescriptor.getAppletClassNames();

        for (String appletClassName : appletClassNames) {
            File appletClassFile = SourceUtil.classNameToClassFile(appletClassName, _classDir);

            // make the jar file with classes in the .class dir it depends on
            AppletJarBuilder appletJarBuilder = new AppletJarBuilder(_classDir, _jarOutputDir, _depFinder);
            Set<String> ssResourceFiles = compDescriptor.getDeclaredAppletFileDependencies(appletClassName);
            Set<File> resourceFiles = FileUtil.findFilesInDirectory(ssResourceFiles, _classDir);

            File appletJarFile = appletJarBuilder.writeJar(appletClassName, resourceFiles);
            appletJars.add(appletJarFile);

            // find jar files the applet depends on via DependencyAnalyzer
            // include jars depended on by other classes in the classDir that
            // the applet depends on
            Set<File> appletClassDeps = _depFinder.getDeepClassDeps(appletClassFile);
            Set<File> appletLibJarDeps = _depFinder.getDeepJarDeps(appletClassDeps);
            appletJars.addAll(appletLibJarDeps);

            // include jars that were specified explicitly in the component
            // annotation for the applet but may not have been picked up
            // by DependencyAnalyzer
            Set<String> appletJarFileBaseNames = compDescriptor.getDeclaredAppletJarDependencies(appletClassName);
            Set<File> explicitAppletJarFiles = FileUtil.findFilesInDirectory(appletJarFileBaseNames, _jarLibDir);
            appletJars.addAll(explicitAppletJarFiles);
        }
        return appletJars;
    }

    @SuppressWarnings("unused")
    private static void logWarn(String str) {
        System.out.println(str);

    }

    private static void logInfo(String str) {
        if (_verbose)
            System.out.println(str);
    }

    /**
     * creates (or verifies they exist) the various directories in the working
     * dir this installer will use to output jar files, descriptors, etc.
     * creates directories on the local filesystem.
     */
    /*
     * private void setupWorkingDir(){
     * 
     * }
     */

    /**
     * finds all components in this installer's class directory and installs
     * them via installComponent(componentClassName).
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws TransmissionException
     * @throws CorruptedDescriptionException
     * 
     **/
    public void installAllComponents() throws IOException, ClassNotFoundException, CorruptedDescriptionException, TransmissionException {

        Set<String> componentClassNames = getAllComponentClassNames();
        for (String compClassName : componentClassNames) {
            logInfo("InstallingComponent: " + compClassName);
            installComponent(compClassName);
        }
    }

    public void uninstallAllComponents() throws IOException, ClassNotFoundException, CorruptedDescriptionException, TransmissionException,
            URISyntaxException {

        Set<String> componentClassNames = getAllComponentClassNames();
        for (String compClassName : componentClassNames) {
            uninstallComponent(compClassName);
        }
    }

    /**
     * searches this ComponentInstaller's classesDirectory for classes that
     * represent Components (as determined by whether or not they have a
     * Component Annotation).
     * 
     * @return a set of the full canonical class names (eg
     *         "org.meandre.myComponent")
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws CorruptedDescriptionException
     * @throws TransmissionException
     */
    private Set<String> getAllComponentClassNames() throws IOException, ClassNotFoundException, CorruptedDescriptionException, TransmissionException {

        Set<String> compClassNames = new HashSet<String>(50);

        Iterator<File> classesDirIter = new FileTreeIterator(_classDir);
        while (classesDirIter.hasNext()) {
            File nextFile = classesDirIter.next();
            if (nextFile.toString().endsWith(".class")) {
                String className = SourceUtil.classFileToClassName(nextFile, _classDir);
                Class<?> klass = SourceUtil.classNameToClass(className, loader);
                if (ComponentSourceDescriptor.isClassAComponent(klass)) {
                    compClassNames.add(className);
                }
            }
        }
        return compClassNames;
    }
    
    public static void setVerbose(boolean verbose) {
        _verbose = verbose;
    }
    
    public static boolean getVerbose() {
        return _verbose;
    }
}
