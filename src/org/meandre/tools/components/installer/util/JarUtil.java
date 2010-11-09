package org.meandre.tools.components.installer.util;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Convenience functions for creating and reading the contents of jar files.
 * These methods open, modify, and close the jar file on disk with every call,
 * making them less efficient than the java.util.jar output streams and 
 * zipEntry methods, but are much easier to use. In particular, this class
 * makes it easier to deal with existing files that will be put into
 * a jar, as opposed to dealing with InputStreams and OutputStreams.
 *
 * @author pgroves
 */


public class JarUtil{

    /**
     * creates an almost empty jar file on disk. the jar file will have a
     * manifest, which will have no entries.
     * 
     * @param jarFile the file locator of where to put the jar file.
     * @throws IOException problem creating jar file
     */
    public static void createJarFile(File jarFile) throws IOException{
        
        //make an empty manifest
        Manifest manifest = new Manifest();
        
        //make a file to write to
        FileOutputStream fos = new FileOutputStream(jarFile);
        
        //write it as a jar
        JarOutputStream jos = new JarOutputStream(fos, manifest);
        jos.close();
        return;
    }
    /**
     * Adds a single file to an existing jar file (like one created by
     * createJarFile()). The name inside the jar file of the contentFile
     * is the string value of the JarEntry produced by filenameToJarEntry. 
     * 
     * <p>This operation requires making a complete copy
     * of all existing contents of the jar file and should therefore be
     * used sparingly. Use addFilesToJarFile if you want to add more than
     * a single file to a jar.
     * 
     * @param jarFile the jar file to be added to
     * @param contentFile the existing file to be added into the jar
     * @param relatvieBaseDir the names of the files inside the jar will be
     * their path relative to this directory name. so for a given file, it's 
     * name in the jar will be everything to the right of this base directory,
     * which should normally start with the system's root dir ("/" or "c:").
     */
    public static void addFileToJarFile(File jarFile, File contentFile, 
            File relativeBaseDir) throws IOException{
        
        JarAdder adder = new JarAdder(jarFile);
        adder.addFile(contentFile, relativeBaseDir);
        adder.finish();
        

        return;
    }

    /**
     * Adds a set of files to a jar file. The name inside the jar file of each 
     * contentFile is the string value of the JarEntry produced by 
     * filenameToJarEntry.
     * 
     * <p>This method requires copying the entire existing contents of the jarFile
     * to a temporary file. It is recommended to put all files to add in a 
     * File Set and call this method exactly once after createJarFile.
     *
     * @param jarFile the jar file to be added to
     * @param contentsFiles the existing files to be added into the jar
     * @param relatvieBaseDir the names of the files inside the jar will be
     * their path relative to this directory name. so for a given file, it's 
     * name in the jar will be everything to the right of this base directory,
     * which should normally start with the system's root dir ("/" or "c:").
     * @throws IOException 
     */
    public static void addFilesToJarFile(File jarFile, Set<File> contentsFiles, 
            File relativeBaseDir) throws IOException{

        JarAdder adder = new JarAdder(jarFile);
        for(File contentFile: contentsFiles){
            adder.addFile(contentFile, relativeBaseDir);
        }
        adder.finish();

        return;
    }

    /**
     * Overwrites the manifest file in the jarFile to the values in the
     * input Manifest object. 
     * @throws IOException 
     *
     *
     */
    public static void setManifest(File jarFile, Manifest manifest) 
        throws IOException{
        JarAdder adder = new JarAdder(jarFile, manifest);
        //the adder constructor copies the new manifest and the existing
        //contents into it's tmpFile, so as soon as the constructor
        //completes we're done.
        adder.finish();
        return;
    }

    /**
     * Reads and returns the Manifest from an existing jar file. Does not
     * affect the file.
     * 
     * @param jarFile an existing jar file on disk.
     * @return a Manifest object
     * @throws IOException
     */
    public static Manifest getManifest(File jarFile) throws IOException{
        FileInputStream fis = new FileInputStream(jarFile);
        JarInputStream jis = new JarInputStream(fis);
        Manifest manifest = jis.getManifest();
        jis.close();
        return manifest;
    }
    

    /**
     * Extracts the entries in a jar file to files in a destination directory.
     * Effectively the same as calling "jar -xf jarFile" on the commandline.
     * The directory hierarchy within the destDir will follow the hierarchy
     * implied by the names of the entries, eg the jar entry with name
     * "foo/bar.txt" will end up as the file 'bar.txt' in the directory 
     * 'destDir/foo/'
     * 
     * @throws IOException Problem reading the jar or creating the output files
     *
     */
    public static void unpackJarFile(File jarFile, File destDir) throws IOException{
        FileInputStream fis = new FileInputStream(jarFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;
        while((entry = zis.getNextEntry()) != null){
            File outputFile = jarEntryToFilename(new JarEntry(entry), destDir);
            //log("unpackJarFile: unpacking: " + outputFile.toString());
            //make the parent directory
            File parentDir = outputFile.getParentFile();
            parentDir.mkdirs();
            //write the file 
            FileOutputStream fos = new FileOutputStream(outputFile);
            copyStreamBytes(zis, fos);
            fos.close();
        }
        zis.close();
    }




    /**
     * Converts the name of an existing file into an appropriate string
     * name for inclusion in a jar file. 
     * 
     * <p>A file called '/foo/bar/blah.txt' with relativeBaseDir='/foo/' will
     * produce a jarEntry with name 'bar/blah.txt'.
     *
     * <p>Note the produced name will always use forward slashes, regardless
     * of whether the filename uses forward or back slashes.
     *
     * @return a jarEntry representation of the name
     * @throws IOException 
     */

    public static String filenameToJarEntryName(File file, 
            File relativeBaseDir) throws IOException{
        String name = file.getCanonicalPath();
        String relativeBaseName = relativeBaseDir.getCanonicalPath() + 
                File.separator;
        //log("absoluteFileName:           " + name);
        //log("absolute relative basename: " + relativeBaseName);
        name = name.replace(relativeBaseName, "");
        //log("name w/ basename removed    " + name);
        name = name.replace(File.separatorChar, '/');
        //log("entry name                  " + name);
        
        return name;
    }
    /**
     * converts a full canonical class name into  the name of the corresponding
     * file entry in a jar file (for the .class file of the class). This is
     * basically a file name that always uses forward slashes (jar files use
     * forward slashes regardless of platform). 
     *
     * Using this method creates the name in a jar file to use for a class
     * that you want to have in a jar that can be used as part of 
     * a classpath.
     * 
     * Example:
     * String className = "org.meandre.Blah";
     * String jarEntryName = JarUtil.classNameToJarEntryName(className);
     * 
     * -> jarEntryName is then "org/meandre/Blah.class"
     * 
     * @param className the name of a class that can be put into a jar file.
     * @return then name of the corresponding .class file that would be
     * in a jar file
     */
    public static String classNameToJarEntryName(String className) {
        String entryName = className.replace('.', '/');
        entryName = entryName + ".class";
        return entryName;
    }
    
    /**
     * for an existing jar file, retrieves the names of all entries in
     * the jar and returns them in a set. The names will look like filenames,
     * but with forward slashes as the file separator, regardless of the
     * platform. That is, the entry names will be the file names that would
     * be created relative to a working directory if the jar file was
     * unpacked in that directory.
     * @throws IOException 
     *
     */
    public static Set<String> getJarEntryNames(File jarFile) throws IOException {

        Set<String> entryNames = new HashSet<String>();
        JarFile jarObj = new JarFile(jarFile);
        Enumeration<JarEntry> obsEntries = jarObj.entries();


  
        while(obsEntries.hasMoreElements()){
            JarEntry entry = obsEntries.nextElement();
            //log("EntryName:  " + entry.toString());
            entryNames.add(entry.getName());
        }
        jarObj.close();
        
    
        /**
        //make an input stream of the existing jar
        FileInputStream fis = new FileInputStream(jarFile);
        JarInputStream jis = new JarInputStream(fis);
            
        //iterate over each jar entry in the existing, and copy it's
        //name to the name set
        JarEntry entry;
        while((entry = jis.getNextJarEntry()) != null){
            log("entryName: " + entry.getName());
            entryNames.add(entry.getName());
        }

        jis.close();*/
        return entryNames;
    }
    /** 
     * the inverse of filenameToJarEntry. 
     *
     * A jarEntry with name 'bar/blah.txt' with relativeBaseDir '/foo/' will
     * produce a File with location '/foo/bar/blah.txt'
     *
     */
    private static File jarEntryToFilename(JarEntry jarEntry, 
                File relativeBaseDir){
        String entryName = jarEntry.getName();
        String filename = entryName.replace('/', File.separatorChar);
        File destFile = new File(relativeBaseDir, filename);
        return destFile;
    }
    
    /**
     * reads the bytes of the inputStream and writes them to the outputStream
     * until the end of the input stream is reached. Does not open, close or
     * otherwise affect either the inputStream or outputStream besides
     * doing the read/write.
     * @throws IOException if problem with the actual read or write
     */
    private static void copyStreamBytes(InputStream inputStream, 
            OutputStream outputStream) throws IOException{
        byte[] buf = new byte[1024];
        int anz;
        while ((anz = inputStream.read(buf)) != -1) {
            outputStream.write(buf, 0, anz);
        }
        return;
    }   
    private static void log(String msg){
        System.out.println("JarUtil." + msg);
    }
    
    /**
     * JarAdder creates a temp file jar, copies all the contents of an
     * existing jar to it, and leaves it open for "adding". The finalize()
     * method closes the tmpfile and overwrites the original file with
     * it.
     * @author pgroves
     *
     */
    private static class JarAdder{
        
        File _originalJarFile;
        File _tmpJarFile;
        File _workingDir;
        JarOutputStream _tmpJarOut;
        
        /**
         * makes a JarAdder for adding to the input jar file, but uses
         * the input manifest instead of the one in the file. Copies
         * all other entries of the original jar file as they are.
         * 
         * @param jarFile the existing jar file (on disk) to add to
         * @param manifest a new manifest for the jar file
         * @throws IOException
         */
        public JarAdder(File jarFile, Manifest manifest) throws IOException{
            realConstructor(jarFile, manifest);
        }
        
        /**
         * Initializes a Jar adder with all contents the same as the original
         * jar.
         * 
         * @param jarFile
         * @throws IOException 
         */
        public JarAdder(File jarFile) throws IOException{
            Manifest manifest = getManifest(jarFile);
            realConstructor(jarFile, manifest);
        }
        
        /**
         * work around the quirkiness of java constructors. Need to call
         * getManifest first before calling the basic constructor, but
         * java makes you call the basic constructor first.
         */
        private void realConstructor(File jarFile, Manifest manifest) 
                throws IOException{
            
            _originalJarFile = jarFile;
            
            _workingDir = jarFile.getParentFile();
            _tmpJarFile = File.createTempFile("JarUtilTmp", "jar", _workingDir);
            FileOutputStream fos = new FileOutputStream(_tmpJarFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            _tmpJarOut = new JarOutputStream(bos, manifest);
            
            
            //copy the existing contents to the tmp file
            
            //make an input stream of the existing jar
            FileInputStream fis = new FileInputStream(jarFile);
            JarInputStream jis = new JarInputStream(fis);
            
            //iterate over each jar entry in the existing, and copy it's
            //name and bytes to the tmp jar file (output stream).
            JarEntry entry;
            while((entry = jis.getNextJarEntry()) != null){
                _tmpJarOut.putNextEntry(entry);
                copyStreamBytes(jis, _tmpJarOut);
                _tmpJarOut.closeEntry();
            }
            
            //done with the input stream of the original, but leaving open
            //the output stream to the tmp file
            jis.close();
        }
        
        public void addFile(File file, File relativeBaseDir) throws IOException{
            String entryName = filenameToJarEntryName(file, relativeBaseDir);
            JarEntry entry = new JarEntry(entryName);
            //log("JarAdder.addFile: adding entry: " + entry.toString());
            
            //open a reader for the file to add
            FileInputStream fis = new FileInputStream(file);
                            
            //add the jarEntry and then write the contents of the file
            _tmpJarOut.putNextEntry(entry);
            copyStreamBytes(fis, _tmpJarOut);
            _tmpJarOut.closeEntry();
            fis.close();
            
        }
        
        /*
         * close the open jar output stream, move the tmp file being written
         * to so it overwrites the original jar file to add to.
         * 
         */
        public void finish() throws IOException{
            _tmpJarOut.close();
            
            //move the old jar to a new tmp file, move the existing tmp file
            //to the old jar name
            File tmp2 = File.createTempFile("JarUtilTmp2", "jar", _workingDir);
            tmp2.delete();
            boolean moveSucceed = _originalJarFile.renameTo(tmp2);
            if(!moveSucceed){
                throw new IOException("Failed to rename the original jar file " +
                        "to a temp file for jarFile: \'" + 
                        _originalJarFile.toString() + "\'");
            }            
            
            moveSucceed = _tmpJarFile.renameTo(_originalJarFile);
            if(!moveSucceed){
                throw new IOException("Failed to rename the tmp jar file " +
                        "to the original jar named \'" + 
                        _originalJarFile.toString() + "\'");
            }
            //if we've succeeded, get rid of the tmp2 version
            tmp2.delete();
        }
    
    }
    /**
     * returns the string "META-INF/MANIFEST.MF".
     * @return the default string-name of the manifest file within a jar. This
     * location is actually defined in jar file spec, so it is always this.
     */
    public static String getManifestEntryName() {
        
        return "META-INF/MANIFEST.MF";
    }



}

