package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.meandre.tools.components.installer.ComponentInstaller;

import com.tonicsystems.jarjar.AbstractDepHandler;
import com.tonicsystems.jarjar.DepFind;
import com.tonicsystems.jarjar.DepHandler;

/**
 * This isolates the dependencies on the JarJar library for finding dependencies
 * amongst .class files and .jar files. These few static methods attempt to do
 * the bare minimum of what must be asked of the JarJar library as it is poorly
 * documented and does not have an api designed for use as an external library.
 * 
 * <p>
 * Typically DependencyAnalyzer should be used by developers to detect
 * dependencies amongst various types of java files.
 * 
 * @author pgroves
 */
public class JarJarWrapper {

    /**
     * For a given .class file, find the other .class files of classes that it
     * directly references.
     * 
     * @throws IOException
     */
    public static Set<File> directClassToClassDependencies(File classFile, File classDirToSearch) throws IOException {

        log(String.format("Detecting class-to-class dependencies for '%s' (searching in %s)", classFile, classDirToSearch));

        // create the classpath string jar jar will understand, using the
        // classesDir
        String sJarJarClasspath = makeJarJarClasspathFromClassDir(classDirToSearch);

        log("sJarJarClasspath: " + sJarJarClasspath);

        // make a classDepHandler and recursively run all the
        // classFileDependencies through it
        ClassDepHandler jarJarHandler = new ClassDepHandler();
        DepFind jarJarAnalyzer = new DepFind();

        try {
            jarJarAnalyzer.run(classFile.toString(), sJarJarClasspath, jarJarHandler);
        }
        catch (RuntimeException e) {
            log("Could not detect dependencies for: " + classFile);
            throw e;
        }

        // turn the string classnames into File objects
        Set<String> classNameDeps = jarJarHandler.getClassSet();
        Set<File> fileDeps = classNameSetToFileSet(classNameDeps, classDirToSearch);

        return fileDeps;

    }

    /**
     * For a given .class file, find any .jar files that contain classes that it
     * directly references.
     * 
     * Odd Behaviour: If a jar file contains the input classFile, that jar file
     * will be returned in the set of Jar Dependencies.
     * 
     * @throws IOException
     */
    public static Set<File> directClassToJarDependencies(File classFile, File jarDirToSearch) throws IOException {

        // create the classpath string jar jar will understand, using the
        // jarDir
        String sJarJarClasspath = makeJarJarClasspathFromJarDir(jarDirToSearch);

        // make a JarDepHandler and run all the classFileDependencies through
        // it
        JarDepHandler jarJarHandler = new JarDepHandler();
        DepFind jarJarAnalyzer = new DepFind();

        try {
            jarJarAnalyzer.run(classFile.toString(), sJarJarClasspath, jarJarHandler);
        }
        catch (RuntimeException e) {
            log("Could not detect dependencies for: " + classFile);
            throw e;
        }
        
        // turn the string filenames into File objects
        // Set<String> fileNameDeps = jarJarHandler.getJarSet();
        // Set<File> fileDeps = fileNameSetToFileSet(fileNameDeps);
        Set<File> fileDeps = jarJarHandler.getJarSet();
        return fileDeps;
    }

    /**
     * For a given .jar file, find any .jar files that contain classes that any
     * of it's own internal classes directly reference. The returned file set
     * will include the input jarFile.
     * 
     * @throws IOException
     */
    public static Set<File> directJarToJarDependencies(File jarFile, File jarDirToSearch) throws IOException {

        // make a jar dep handler for finding the deps of each jar
        JarDepHandler jarJarHandler = new JarDepHandler();
        DepFind jarJarAnalyzer = new DepFind();

        String sJarJarClasspath = makeJarJarClasspathFromJarDir(jarDirToSearch);

        try {
            jarJarAnalyzer.run(jarFile.toString(), sJarJarClasspath, jarJarHandler);
        }
        catch (RuntimeException e) {
            log("Could not detect dependencies for: " + jarFile);
            throw e;
        }
        
        Set<File> foundDeps = jarJarHandler.getJarSet();
        foundDeps.add(jarFile);
        log("directJarToJarDependencies: found " + foundDeps.size() + " deps for jar: " + jarFile.getName());
        return foundDeps;
    }

    private static String makeJarJarClasspathFromClassDir(File classesDir) {
        String classpathStr = classesDir.getAbsolutePath();
        return classpathStr;
    }

    private static String makeJarJarClasspathFromJars(Set<File> jarFiles) throws IOException {

        StringBuffer classpathBuf = new StringBuffer();
        for (File jarFile : jarFiles) {
            classpathBuf.append(jarFile.getCanonicalPath());
            classpathBuf.append(File.pathSeparator);
        }
        // remove the trailing path separator character if there
        // is more than zero jar file
        if (!jarFiles.isEmpty()) {
            classpathBuf.deleteCharAt(classpathBuf.length() - 1);
        }
        return classpathBuf.toString();
    }

    private static String makeJarJarClasspathFromJarDir(File jarsDir) throws IOException {
        Set<File> jarFiles = new HashSet<File>();
        Iterator<File> jarIter = new FileTreeIterator(jarsDir);
        while (jarIter.hasNext()) {
            File nextFile = jarIter.next();
            if (nextFile.toString().endsWith(".jar")) {
                jarFiles.add(nextFile);
            }
        }
        String cp = makeJarJarClasspathFromJars(jarFiles);
        return cp;
    }

    /**
     * convert a Set of strings that are filenames to a set of File objects
     */
    @SuppressWarnings("unused")
    private static Set<File> fileNameSetToFileSet(Set<String> fileNames) {
        Set<File> files = new HashSet<File>();
        for (String fileName : fileNames) {
            File file = new File(fileName);
            files.add(file);
        }
        return files;
    }

    /**
     * convert a Set of strings that are classnames to a set of File objects
     */
    private static Set<File> classNameSetToFileSet(Set<String> classNames, File classDir) {
        Set<File> files = new HashSet<File>();
        for (String className : classNames) {
            File file = SourceUtil.classNameToClassFile(className, classDir);
            files.add(file);
        }
        return files;
    }

    private static void log(String msg) {
        if (ComponentInstaller.getVerbose())
            System.out.println("JarJarWrapper." + msg);
    }

    /**
     * a JarJar DepHandler that collects visited classes and saves them in a
     * Set.
     */
    private static class ClassDepHandler extends AbstractDepHandler {

        /** collects the class files passed to this handler by jar jar */
        Set<String> _handledClasses = null;

        protected ClassDepHandler() {
            super(DepHandler.LEVEL_CLASS);
            _handledClasses = new HashSet<String>(10);
        }

        @Override
        public void handleStart() throws IOException {
            log("ClassDepHandler.handle: Begin");
        }

        @Override
        protected void handle(String from, String to) throws IOException {
            log("ClassDepHandler.handle: From: " + from);
            log("ClassDepHandler.handle: To: " + to);
            // don't worry about duplicates, the hashset will only show
            // one of each
            _handledClasses.add(to);
        }

        @Override
        public void handleEnd() throws IOException {
            log("ClassDepHandler.handle: End");
        }

        public Set<String> getClassSet() {
            return _handledClasses;
        }
    }

    /**
     * a JarJar DepHandler that collects visited jars and saves them in a Set.
     */
    private static class JarDepHandler extends AbstractDepHandler {

        /** collects the jar files passed to this handler by jar jar */
        Set<File> _handledJars = null;

        public JarDepHandler() {
            super(DepHandler.LEVEL_JAR);
            _handledJars = new HashSet<File>(10);
        }

        @Override
        protected void handle(String from, String to) throws IOException {
            // don't worry about duplicates, the hashset will only show
            // one of each jar
            File fromFile = new File(from);
            File toFile = new File(to);
            log("JarDepHandler.handle: Deduced That: \'" + fromFile.getName() + "\' depends on \'" + toFile.getName());
            // System.out.println("JarDepHandler.handle: To: " + to);
            _handledJars.add(toFile);
            // System.out.println("JarDepHandler.handle: End");
        }

        public Set<File> getJarSet() {
            return _handledJars;
        }

        @SuppressWarnings("unused")
        public void reset() {
            _handledJars.clear();
        }
    }
}
