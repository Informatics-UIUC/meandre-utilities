package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Detects and stores the dependencies between all class files and jar files in
 * a project. A DependencyAnalyzer is initialized with a jars directory and a
 * classes directory, at which point it analyzes all .class files and .jar files
 * for their direct dependencies. The analyzer can then be queried for the
 * direct (shallow) or recursive (deep) dependencies between .class files and
 * other .class files, .class files and .jar files, and .jar files and other
 * .jar files.
 * 
 * <p>
 * All dependencies are deduced at initialization and stored internally in a set
 * of HashMaps. Calls to the get* methods are therefore very fast, while
 * constructing a new analyzer is very slow.
 * 
 * @author pgroves
 */
public class DependencyAnalyzer {

    /**
     * dependency map from .class files to .class files in the classDir
     * directory
     */
    DepMap _classClassDeps;

    /**
     * dependency map from .class files in the classDir to .jar files in the
     * jarDir
     */
    DepMap _classJarDeps;

    /**
     * dependency map from .jar files to .jar files in the jarDir
     */
    DepMap _jarJarDeps;

    /**
     * constructor that only initializes internal fields. use add*Deps methods
     * to populate.
     */
    public DependencyAnalyzer() {
        _classClassDeps = new DepMap();
        _classJarDeps = new DepMap();
        _jarJarDeps = new DepMap();
    }

    /**
     * Reads the contents of a directory of .jar files and one of .class files
     * and constructs an internal dependency graph.
     * 
     * Note: the jarDir and classDir may be 'null'. If null, any request for
     * dependencies of the respective type (jar or class) will return an empty
     * set.
     * 
     * @param jarDir
     *            a top level directory with jar files in it to include in the
     *            analysis. only jar files in this directory will be candidates
     *            for get*JarDeps* methods, and only these jars can be queried.
     * @param classDir
     *            a top level directory with class files in it to include in the
     *            analysis. only class files in this directory will be
     *            candidates for get*ClassDeps* methods, and only these classes
     *            can be queried.
     * @throws IOException
     */
    public DependencyAnalyzer(File jarDir, File classDir) throws IOException {
        this();
        
        if (!jarDir.exists())
            throw new FileNotFoundException(jarDir.toString());
        
        if (!classDir.exists())
            throw new FileNotFoundException(classDir.toString());
        
        addClassToClassDeps(classDir);
        addClassToJarDeps(classDir, jarDir);
        addJarToJarDeps(jarDir);

    }

    public void addClassToClassDeps(File classDir) throws IOException {
        if (classDir != null) {
            log("DepAnalyzer: populating class-to-class deps");
            populateClassClassDeps(_classClassDeps, classDir);
        }
    }

    public void addClassToJarDeps(File classDir, File jarDir) throws IOException {
        if ((classDir != null) && (jarDir != null)) {
            log("DepAnalyzer: populating class-to-jar deps");
            populateClassJarDeps(_classJarDeps, classDir, jarDir);
        }
    }

    public void addJarToJarDeps(File jarDir) throws IOException {
        if (jarDir != null) {
            log("DepAnalyzer: populating jar-to-jar deps");
            populateJarJarDeps(_jarJarDeps, jarDir);
        }
    }

    /**
     * For a .class file in this analyzer's classDir, retrieve all .class files
     * it directly depends on. The returned set will include the target class
     * file itself.
     */
    public Set<File> getShallowClassDeps(File classFile) throws IOException {
        return _classClassDeps.getShallowDeps(classFile);
    }

    /**
     * Same as getShallowClassDeps(File), but aggregates the dependencies for
     * all files in the input set.
     **/
    public Set<File> getShallowClassDeps(Set<File> classFiles) throws IOException {
        return _classClassDeps.getShallowDeps(classFiles);
    }

    /**
     * For a .class file in this analyzer's classDir, retrieve all .class files
     * it depends on, including all indirect, recursive dependencies
     * (dependencies of dependencies). The returned set will include the target
     * class file itself.
     */
    public Set<File> getDeepClassDeps(File classFile) throws IOException {
        return _classClassDeps.getDeepDeps(classFile);
    }

    /**
     * Same as getDeepClassDeps(File), but aggregates the dependencies for all
     * files in the input set.
     **/
    public Set<File> getDeepClassDeps(Set<File> classFiles) throws IOException {
        return _classClassDeps.getDeepDeps(classFiles);
    }

    /**
     * For a .class file in this analyzer's classDir, retrieve all .jar files
     * from the jarDir it directly depends on.
     **/
    public Set<File> getShallowJarDeps(File classFile) throws IOException {
        return _classJarDeps.getShallowDeps(classFile);
    }

    /**
     * Same as getShallowJarDeps(File), but aggregates the dependencies for all
     * files in the input set.
     **/
    public Set<File> getShallowJarDeps(Set<File> classFiles) throws IOException {
        return _classJarDeps.getShallowDeps(classFiles);
    }

    /*
     * For a .class file in this analyzer's classDir, retrieve all .jar files it
     * depends on, including all indirect, recursive dependencies (dependencies
     * of dependencies) from this analyzer's jarDir.
     * 
     * <p>Note that this returns the deep *jar* dependencies for the class file,
     * but not any jar dependencies of the class file's *class* dependencies.
     */
    public Set<File> getDeepJarDeps(File classFile) throws IOException {
        Set<File> shallowJarDeps = _classJarDeps.getShallowDeps(classFile);
        Set<File> deepJarDeps = getDeepJarDepsOfJars(shallowJarDeps);
        return deepJarDeps;
    }

    /**
     * Same as getDeepJarDeps(File), but aggregates the dependencies for all
     * files in the input set.
     **/
    public Set<File> getDeepJarDeps(Set<File> classFiles) throws IOException {
        Set<File> shallowJarDeps = _classJarDeps.getShallowDeps(classFiles);
        Set<File> deepJarDeps = getDeepJarDepsOfJars(shallowJarDeps);
        return deepJarDeps;
    }

    /**
     * For a .jar file in this analyzer's jarDir, retrieve all .jar files from
     * the jarDir it directly depends on.
     **/
    public Set<File> getShallowJarDepsOfJar(File jarFile) throws IOException {
        return _jarJarDeps.getShallowDeps(jarFile);
    }

    /**
     * Same as getShallowJarDepsOfJar(File), but aggregates the dependencies for
     * all files in the input set.
     **/

    public Set<File> getShallowJarDepsOfJars(Set<File> jarFiles) throws IOException {
        return _jarJarDeps.getShallowDeps(jarFiles);
    }

    /*
     * For a .jar file in this analyzer's jarDir, retrieve all .jar files it
     * depends on, including all indirect, recursive dependencies (dependencies
     * of dependencies) from this analyzer's jarDir.
     */
    public Set<File> getDeepJarDepsOfJar(File jarFile) throws IOException {
        return _jarJarDeps.getDeepDeps(jarFile);
    }

    /**
     * Same as getDeepJarDepsOfJar(File), but aggregates the dependencies for
     * all files in the input set.
     **/
    public Set<File> getDeepJarDepsOfJars(Set<File> jarFiles) throws IOException {
        return _jarJarDeps.getDeepDeps(jarFiles);
    }

    /**
     * For each class file in the classesDir, adds an entry into the depMap from
     * the .class file (the key) to the set of class files it depends on
     * directly (the set is the value in the map). Only adds shallow (direct)
     * dependencies.
     * 
     * @throws IOException
     */
    private void populateClassClassDeps(DepMap depMap, File classDir) throws IOException {
        // Set<File> set = findClassClassDeps(classDir, classDir);
        // log(set.toString());
        log("populateClassClassDeps: Begin");
        Iterator<File> allClassFilesIter = new FileTreeIterator(classDir);
        while (allClassFilesIter.hasNext()) {
            File nextFile = allClassFilesIter.next();
            log("populateClassClassDeps: analyzing file:" + nextFile.toString());
            if (nextFile.toString().endsWith(".class")) {
                Set<File> localDeps = findClassClassDeps(nextFile, classDir);
                depMap.declareTarget(nextFile);
                for (File depFile : localDeps) {
                    depMap.add(nextFile, depFile);
                }
            }
        }
        log("populateClassClassDeps: End");
    }

    /**
     * for a class file, find the other classes in the classDir that it depends
     * on.
     * 
     * @param classFile
     * @param classDir
     * @return a set of .class files
     * @throws IOException
     */
    protected Set<File> findClassClassDeps(File classFile, File classDir) throws IOException {

        Set<File> localDeps = JarJarWrapper.directClassToClassDependencies(classFile, classDir);
        return localDeps;
    }

    /**
     * Similar to populateClassClassDeps, but does the mapping from .class files
     * to the .jar files they depend on directly.
     * 
     * @throws IOException
     */
    private void populateClassJarDeps(DepMap depMap, File classDir, File jarDir) throws IOException {

        Iterator<File> allClassFilesIter = new FileTreeIterator(classDir);
        while (allClassFilesIter.hasNext()) {
            File nextFile = allClassFilesIter.next();
            if (nextFile.toString().endsWith(".class")) {
                Set<File> localDeps = findClassJarDeps(nextFile, classDir, jarDir);
                depMap.declareTarget(nextFile);
                for (File depFile : localDeps) {
                    depMap.add(nextFile, depFile);
                }
            }
        }
        return;
    }

    /**
     * for a class file, find the jars in the libDir that it depends on
     * directly.
     * 
     * @param classFile
     * @param jarDir
     * @return a set of .jar files
     * @throws IOException
     */
    protected Set<File> findClassJarDeps(File classFile, File classDir, File jarDir) throws IOException {

        Set<File> localDeps = JarJarWrapper.directClassToJarDependencies(classFile, jarDir);
        return localDeps;
    }

    /**
     * Similar to populateClassClassDeps, but does the mapping from .jar files
     * to the other .jar files they depend on directly.
     * 
     * @throws IOException
     */

    private void populateJarJarDeps(DepMap depMap, File jarDir) throws IOException {

        Iterator<File> allJarFilesIter = new FileTreeIterator(jarDir);
        while (allJarFilesIter.hasNext()) {
            File nextFile = allJarFilesIter.next();
            if (nextFile.toString().endsWith(".jar")) {
                Set<File> localDeps = findJarJarDeps(nextFile, jarDir);
                depMap.declareTarget(nextFile);
                for (File depFile : localDeps) {
                    depMap.add(nextFile, depFile);
                }
            }
        }
        return;
    }

    /**
     * for a jar file, find the jars in the libDir that it depends on directly.
     * 
     * @param classFile
     * @param jarDir
     * @return a set of .jar files
     * @throws IOException
     */
    protected Set<File> findJarJarDeps(File jarFile, File jarDir) throws IOException {

        Set<File> localDeps = JarJarWrapper.directJarToJarDependencies(jarFile, jarDir);
        return localDeps;
    }

    protected static void log(String msg) {
        System.out.println("DependencyAnalyzer." + msg);
    }

    /**
     * holds dependency graph information on a set of files and can traverse the
     * graph to find all dependencies of a particular file.
     * 
     * Note the DepMap can only have dependencies added to it, there is no
     * removing a dependency after it has been added. Adding a dependency more
     * than once has no ill effect.
     */
    private class DepMap {

        HashMap<File, Set<File>> _deps;

        public DepMap() {
            _deps = new HashMap<File, Set<File>>(100);
        }

        /**
         * creates an empty set of dependencies for the input target file. this
         * should be called before calls to 'add()' that use targetFile as a
         * target. this is especially necessary to handle situations where this
         * DepMap is responsible for the dependencies of targetFile, but
         * targetFile has no dependencies so it's dependencies should be an
         * empty set and not null.
         * 
         * @param targetFile
         * @throws IOException
         */
        public void declareTarget(File targetFile) throws IOException {
            targetFile = targetFile.getCanonicalFile();
            // log("DepMap.declareTarget:\n  " + targetFile.toString());
            _deps.put(targetFile, new HashSet<File>(10));
        }

        /**
         * adds a dependency link from a target file to a file it "depends on"
         * 
         * @throws IOException
         */
        public void add(File targetFile, File dependsOnFile) throws IOException {
            targetFile = targetFile.getCanonicalFile();
            dependsOnFile = dependsOnFile.getCanonicalFile();

            if (_deps.containsKey(targetFile)) {
                // log("DepMap.add: \n  target   =" + targetFile.toString() +
                // "\n  dependsOn=" + dependsOnFile.toString());
                _deps.get(targetFile).add(dependsOnFile);
            } else {
                _deps.put(targetFile, new HashSet<File>(10));
                // recurse to get back to the 'if' above
                this.add(targetFile, dependsOnFile);
            }
            return;
        }

        /**
         * get the set of files a target file depends directly on
         * 
         * @throws IOException
         */
        public Set<File> getShallowDeps(File targetFile) throws IOException {
            targetFile = targetFile.getCanonicalFile();
            if (!_deps.containsKey(targetFile)) {
                throw new IllegalArgumentException("Requesting dependencies for" + " an unknown target. This DependencyAnalyzer does not have "
                        + "dependencies for : " + targetFile.toString());
            }
            Set<File> shallowDeps = _deps.get(targetFile);
            return shallowDeps;
        }

        /**
         * get the aggregated set of files depended on by a set of target files.
         * 
         * @throws IOException
         */
        public Set<File> getShallowDeps(Set<File> targetFiles) throws IOException {
            targetFiles = fileSetToAbsoluteFiles(targetFiles);
            HashSet<File> allShallowDeps = new HashSet<File>(20);
            for (File currentFile : targetFiles) {
                Set<File> localDeps = getShallowDeps(currentFile);
                allShallowDeps.addAll(localDeps);
            }
            return allShallowDeps;
        }

        /**
         * find all dependencies, and all dependencies of dependencies, of a
         * target file.
         * 
         * @throws IOException
         */
        public Set<File> getDeepDeps(File targetFile) throws IOException {
            targetFile = targetFile.getCanonicalFile();
            // this will be a queue to hold files whose dependencies need
            // to be found
            LinkedList<File> pendingQueue = new LinkedList<File>();
            // add the target to start the dependency search
            pendingQueue.addFirst(targetFile);

            // this will hold files that have already been visited (the
            // list of dependency files)
            Set<File> acceptedSet = new HashSet<File>();
            acceptedSet.add(targetFile);

            while (!pendingQueue.isEmpty()) {
                File nextFile = pendingQueue.removeLast();
                // log("DependencyAnalyzer.getDeepDeps: processing " +
                // nextFile.toString());
                Set<File> localDeps = _deps.get(nextFile);
                for (File currentDep : localDeps) {
                    if (!acceptedSet.contains(currentDep)) {
                        acceptedSet.add(currentDep);
                        pendingQueue.addFirst(currentDep);
                    }
                }
            }
            return acceptedSet;
        }

        /**
         * aggregate the deep dependencies found by getDeepDeps(File) for a set
         * of target files.
         * 
         * @throws IOException
         */
        public Set<File> getDeepDeps(Set<File> targetFiles) throws IOException {
            targetFiles = fileSetToAbsoluteFiles(targetFiles);
            HashSet<File> allDeepDeps = new HashSet<File>(50);
            for (File currentFile : targetFiles) {
                Set<File> localDeps = getDeepDeps(currentFile);
                allDeepDeps.addAll(localDeps);
            }
            return allDeepDeps;
        }

        /**
         * returns a new Set<File>, where all the input files in relativeFiles
         * are replaced by their absolutePath files.
         * 
         * @param relativeFiles
         * @return
         * @throws IOException
         */
        private Set<File> fileSetToAbsoluteFiles(Set<File> relativeFiles) throws IOException {
            Set<File> absFiles = new HashSet<File>();
            for (File relFile : relativeFiles) {
                absFiles.add(relFile.getCanonicalFile());
            }
            return absFiles;
        }

    }

}
