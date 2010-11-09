package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class FileUtil {

    public static void copy(File source, File destination) throws IOException {

        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(destination);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();

    }

    /**
     * takes a file (name), and returns the path relative to the relative
     * basedir. eg: myFile = new File('/bar/foo/blah.txt'); relativeBase = new
     * File('/bar/'); toRelativePath(myFile, relativeBase) //// returns
     * 'foo/blah.txt'
     * 
     * @throws IOException
     *             if the file or relativeBaseDir fail when getAbsolutePath() is
     *             called on them
     * 
     */
    public static String toRelativePath(File file, File relativeBaseDir) throws IOException {
        String fullFileName = file.getAbsolutePath().toString();
        String fullBaseName = relativeBaseDir.getAbsolutePath().toString();

        assert (fullFileName.startsWith(fullBaseName));

        String relPath = fullFileName.substring(fullBaseName.length() + 1);
        return relPath;

    }

    public static void copyRecursive(File dir, File destinationDir) throws IOException {

        assert (dir.exists());
        assert (dir.isDirectory());
        assert (destinationDir.exists());
        assert (destinationDir.isDirectory());

        // we want the file copies to include (start with) the name
        // of the source directory, so we need to be able to make
        // relative paths vs. the source directory's parent
        File sourceParentDir = dir.getParentFile();

        // copy the directory tree structure. the iterator is top to
        // bottom, so higher level dirs will be made before attempting
        // their children
        Iterator<File> sourceDirIter = new DirectoryTreeIterator(dir);
        while (sourceDirIter.hasNext()) {
            File nextSrcDir = sourceDirIter.next();
            String relPath = toRelativePath(nextSrcDir, sourceParentDir);
            File destDir = new File(destinationDir, relPath);
            destDir.mkdir();
        }

        // with the directory tree copied, the regular files can be
        // folded in
        Iterator<File> sourceFileIter = new FileTreeIterator(dir);
        while (sourceFileIter.hasNext()) {
            File nextSrcFile = sourceFileIter.next();
            String relPath = toRelativePath(nextSrcFile, sourceParentDir);
            File destFile = new File(destinationDir, relPath);
            copy(nextSrcFile, destFile);
        }
        return;

    }

    /** rm -rf */
    public static void deleteDirRecursive(File dir) throws IOException {
        // get rid of the regular files
        Iterator<File> regFilesIter = new FileTreeIterator(dir);
        while (regFilesIter.hasNext()) {
            regFilesIter.next().delete();
        }

        // collect the directories, the iterator returns them from top
        // to bottom, we'll have to delete them bottum to top (only empty
        // directories can be deleted)
        Iterator<File> dirIter = new DirectoryTreeIterator(dir);
        LinkedList<File> dirStack = new LinkedList<File>();
        while (dirIter.hasNext()) {
            dirStack.addFirst(dirIter.next());
        }
        // now delete in order of the list
        while (dirStack.size() > 0) {
            File nextFile = dirStack.removeFirst();
            nextFile.delete();
        }
    }

    /**
     * for a file with a given name, find the first instance of it by
     * recursively searching a searchDir and it's subdirectories.
     * 
     * 
     * For example, find jar files in a lib directory of arbitrary directory
     * structure.
     * 
     * @return a File locator of the found file, or null if not found.
     * @throws FileNotFoundException
     */
    public static File findFileInDirectory(String baseFileName, File searchDir) throws FileNotFoundException {

        Iterator<File> fileIter = new FileTreeIterator(searchDir);
        while (fileIter.hasNext()) {
            File testFile = fileIter.next();
            if (testFile.toString().endsWith(baseFileName)) {
                return testFile;
            }
        }
        // wasn't found, return nothing
        return null;
    }

    /**
     * searches through a directory and finds an instance of a file with each of
     * the inputfileBaseNames. will search recursively through subdirectories of
     * searcDir.
     * 
     * eg, searchDir will be something like: new File("./meandre/lib"), and a
     * basename will be something like: new String("jena.jar");
     * 
     * @throws FileNotFoundException
     */
    public static Set<File> findFilesInDirectory(Set<String> fileBaseNames, File searchDir) throws FileNotFoundException {
        Set<File> foundFiles = new HashSet<File>();
        for (String baseName : fileBaseNames) {
            File foundFile = findFileInDirectory(baseName, searchDir);
            if (foundFile == null) {
                throw new FileNotFoundException("did not find file with basename=\'" + baseName + "\'" + " in directory \'" + searchDir.toString()
                        + "\'");
            } else {
                foundFiles.add(foundFile);
            }
        }
        return foundFiles;
    }

    /**
     * tests if a candidate file is 'stale' relative to a reference file. The
     * candidate is stale if one of the following is true: 1)The timestamp of
     * the candidate is OLDER than the reference file. 2)The candidate file does
     * not exist.
     * 
     * @param candidate
     * @param reference
     * @return whether the file is stale
     * @throws FileNotFoundException
     *             if the reference file does not exist
     */
    public static boolean isFileStale(File candidate, File reference) throws FileNotFoundException {

        if (!reference.exists()) {
            throw new FileNotFoundException(reference.toString());
        }
        long refTimeStamp = reference.lastModified();
        if (!candidate.exists()) {
            // not existing is considered 'stale'
            return true;
        }
        long candTimeStamp = candidate.lastModified();
        // logWarn("candTimeStamp: " + candTimeStamp);
        // logWarn("refTimeStamp: " + refTimeStamp);
        boolean isStale = refTimeStamp > candTimeStamp;
        return isStale;
    }

    private static void logWarn(String msg) {
        System.out.println(msg);
    }

}
