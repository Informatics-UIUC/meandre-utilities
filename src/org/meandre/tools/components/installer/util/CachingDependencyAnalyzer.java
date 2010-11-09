package org.meandre.tools.components.installer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class CachingDependencyAnalyzer extends DependencyAnalyzer {

    public static final String DEPENDENCY_CACHE = "dependencies-cache";
    public static final String CLASS_TO_CLASS = "class-to-class";
    public static final String CLASS_TO_JAR = "class-to-jar";
    public static final String JAR_TO_JAR = "jar-to-jar";

    /** toplevel dir for cached artifacts */
    File _cacheDir;

    File _classClassDir;
    File _classJarDir;
    File _jarJarDir;

    /**
     * save a running tally of dep files that have been written b/c the cached
     * copy was not found or was found but stale. for testing
     */
    Set<File> _depFilesWritten;

    /**
     * a running tally of dep files that were useable when one of the finde*Dep
     * methods was run. for testing
     */
    Set<File> _depFilesFoundUseable;

    public CachingDependencyAnalyzer(File jarDir, File classDir, File cacheDir) throws IOException {
        super();

        if (!(cacheDir.exists() && cacheDir.isDirectory())) {
            throw new FileNotFoundException("Dependency cache directory " + "either did not exist or is not a directory: " + cacheDir.toString());
        }
        // put all our artifacts under a single directory in the directory we
        // were given
        _cacheDir = new File(cacheDir, DEPENDENCY_CACHE);
        _cacheDir.mkdir();

        // the subdirectories for the different kind of directories
        _classClassDir = new File(_cacheDir, CLASS_TO_CLASS);
        _classClassDir.mkdir();
        _classJarDir = new File(_cacheDir, CLASS_TO_JAR);
        _classJarDir.mkdir();
        _jarJarDir = new File(_cacheDir, JAR_TO_JAR);
        _jarJarDir.mkdir();

        _depFilesFoundUseable = new HashSet<File>();
        _depFilesWritten = new HashSet<File>();

        // delegate back to the superclass to populate the in memory
        // dependencies sets. these methods will call the find*Deps methods
        // overridden by this class
        super.addClassToClassDeps(classDir);
        super.addClassToJarDeps(classDir, jarDir);
        super.addJarToJarDeps(jarDir);
    }

    @Override
    protected Set<File> findClassClassDeps(File classFile, File classDir) throws IOException {

        String className = SourceUtil.classFileToClassName(classFile, classDir);

        File cacheFile = getCacheFile(className, _classClassDir);
        Set<File> deps = null;

        // if the cache file exists, has valid contents, and is not
        // stale relative to the classFile
        if (isValidCacheFile(cacheFile) && !isStale(cacheFile, classFile)) {
            // use the contents of the cache file for dependencies
            deps = parseCacheFile(cacheFile);
            _depFilesFoundUseable.add(cacheFile);
        } else {
            // find the dependencies in the way our superclass does and generate
            // a new cachefile using those files
            deps = super.findClassClassDeps(classFile, classDir);
            writeCacheFile(cacheFile, deps);
            _depFilesWritten.add(cacheFile);
        }
        return deps;
    }

    @Override
    protected Set<File> findClassJarDeps(File classFile, File classDir, File jarDir) throws IOException {

        String className = SourceUtil.classFileToClassName(classFile, classDir);

        File cacheFile = getCacheFile(className, _classJarDir);
        Set<File> deps = null;

        // if the cache file exists, has valid contents, and is not
        // stale relative to the classFile
        if (isValidCacheFile(cacheFile) && !isStale(cacheFile, classFile)) {
            // use the contents of the cache file for dependencies
            deps = parseCacheFile(cacheFile);
            // _depFilesFoundUseable.add(cacheFile);
        } else {
            // find the dependencies in the way our superclass does and generate
            // a new cachefile using those files
            deps = super.findClassJarDeps(classFile, classDir, jarDir);
            writeCacheFile(cacheFile, deps);
            // _depFilesWritten.add(cacheFile);
        }
        return deps;

    }

    @Override
    protected Set<File> findJarJarDeps(File jarFile, File jarDir) throws IOException {

        String baseName = jarFile.getName();

        File cacheFile = getCacheFile(baseName, _jarJarDir);
        Set<File> deps = null;

        // if the cache file exists, has valid contents, and is not
        // stale relative to the classFile
        if (isValidCacheFile(cacheFile) && !isStale(cacheFile, jarFile)) {
            // use the contents of the cache file for dependencies
            deps = parseCacheFile(cacheFile);
            // _depFilesFoundUseable.add(cacheFile);
        } else {
            // find the dependencies in the way our superclass does and generate
            // a new cachefile using those files
            deps = super.findJarJarDeps(jarFile, jarDir);
            writeCacheFile(cacheFile, deps);
            // _depFilesWritten.add(cacheFile);
        }
        return deps;
    }

    /**
     * a listing of all .dep files written by this caching dependency analyzer
     * since it was instantiated. used for testing.
     */
    protected Set<File> getClassClassDepFilesWritten() {
        return _depFilesWritten;
    }

    /**
     * a listing of all .dep files found by this caching dep analyzer since it
     * was instantiated. used for testing.
     */
    protected Set<File> getClassClassDepFilesFoundUseable() {
        return _depFilesFoundUseable;
    }

    /**
     * writes the filenames, one per line, into the file. will overwrite the
     * file if it exists.
     * 
     * @param cacheFile
     * @param contents
     * @throws FileNotFoundException
     *             if the directory doesn't exist?
     */
    private void writeCacheFile(File cacheFile, Set<File> contents) throws FileNotFoundException {

        PrintWriter writer = new PrintWriter(cacheFile);
        for (File depFile : contents) {
            writer.println(depFile.toString());
        }
        writer.close();
    }

    /**
     * reads the filenames out of a cacheFile and returns them as a set of
     * files.
     * 
     * @param cacheFile
     * @return the list of files in the cacheFile (which are the dependencies)
     * @throws IOException
     */
    private Set<File> parseCacheFile(File cacheFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(cacheFile));
        Set<File> depFiles = new HashSet<File>();
        while (reader.ready()) {
            String line = reader.readLine();
            depFiles.add(new File(line));
        }
        reader.close();
        return depFiles;
    }

    private boolean isStale(File generatedFile, File referenceFile) throws FileNotFoundException {

        boolean isStale = FileUtil.isFileStale(generatedFile, referenceFile);
        if (isStale) {
            log("Stale .dep file: " + generatedFile);
        }
        return isStale;
    }

    /**
     * tests if a file is a valid cache file to use. it is valid if the file
     * exists and can be parsed by our parser.
     */
    private boolean isValidCacheFile(File cacheFile) {
        // if it exists
        if (!cacheFile.exists()) {
            return false;
        }
        // and it is parseable
        try {
            parseCacheFile(cacheFile);
        }
        catch (Exception e) {
            return false;
        }
        // then it is valid
        return true;
    }

    /**
     * generates a filename following a consistent naming scheme given a source
     * basename and the name of the directory the cacheFile will be written to.
     * 
     * @param baseName
     * @param cacheDir
     * @return
     */
    private File getCacheFile(String baseName, File cacheDir) {
        String fileName = baseName + ".dep";
        File cacheFile = new File(cacheDir, fileName);
        return cacheFile;
    }
}
