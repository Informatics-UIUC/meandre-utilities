package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.IOException;

/**
 * convenience functions for dealing with paths of java source code and
 * compiled class files.
 *
 */

public class SourceUtil{

    public static Class<?> sourceFileToClass(File sourceFile,
    		File sourceFileDir, ClassLoader loader) throws ClassNotFoundException, IOException{

        String className = sourceFileToClassName(sourceFile, sourceFileDir);
        System.out.println("className is: " + className);
        Class<?> klass = Class.forName(className, true, loader);
        return klass;
    }
    
    public static File sourceFileToClassFile(File sourceFile, 
    		File sourceFileDir, File classFileDir, ClassLoader loader) 
    		throws ClassNotFoundException, IOException{
    	Class<?> klass = sourceFileToClass(sourceFile, sourceFileDir, loader);
    	File classFile = classToClassFile(klass, classFileDir);
    	return classFile;
    }

    /**
     * translates the name of a source file into a fully qualified
     * class name (with package). Operates over the file name only,
     * so it also requires the source directory root that the
     * source file exists in.
     *
     * eg.
     * inputs:
     *      sourceFile = new File("/blah/blah/src/org/meandre/Example.java")
     *      sourceDir = new File("/blah/blah/src/")
     *
     * returns:
     *       "org.meandre.Example"
     *
     */
     
    public static String sourceFileToClassName(File sourceFile, 
            File sourceDir) throws IOException {

        assert(isSourceFile(sourceFile));

        String sSourceFile = sourceFile.getCanonicalPath();
        String sSourceDir = sourceDir.getCanonicalPath();

        //bail out if the full source file path doesn't begin
        //with the source directory path
        assert(sSourceFile.startsWith(sSourceDir));

        //get everything after the source directory part
        String sPackagedPath = sSourceFile.substring(sSourceDir.length() + 1);

        //trim off the '.java' at the end
        int lastKeeperIndex = sPackagedPath.length() - (".java".length()) - 1;
        sPackagedPath = sPackagedPath.substring(0, lastKeeperIndex + 1);

        //replace slashes with '.'
        String sPackagedName = sPackagedPath.replace(File.separatorChar, '.');

        return sPackagedName;
    }

    /**
	 * given a fully qualified className (with package prefix), returns
	 * the location of it's source code file in the srcDir.
	 *
	 * Does NOT verify that the file is actually there, or if it is
	 * a valid source code file.
	 */
	public static File classNameToSourceFile(String className, File srcDir)
			throws IOException{
	    assert(srcDir.isDirectory());
	    String sFileSuffix = className.replace('.', File.separatorChar);
	    sFileSuffix += ".java";
	    File srcFile = new File(srcDir, sFileSuffix);
	    return srcFile;
	}

	/**
     * for a given (fully qualified with package) name of a class, creates
     * a File locator for the compiled class file assuming the input
     * classesDir as the root.
     *
     * eg.
     * inputs:
     *      className = "org.meandre.Example"
     *      classesDir = "/blah/blah/bin"
     * returns: 
     *      new File("/blah/blah/bin/org/meandre/Example.class")
     *      
     */
    public static File classNameToClassFile(String className, 
            File classesDir){

        String sPathSuffix = className.replace('.', File.separatorChar);
        sPathSuffix = sPathSuffix + ".class";
        File classFile = new File(classesDir, sPathSuffix);
        return classFile;

    }

    public static Class<?> classNameToClass(String className, ClassLoader loader) 
    		throws ClassNotFoundException{
		Class<?> klass = Class.forName(className, true, loader);
    	return klass;
	}

	public static File classToClassFile(Class<?> klass, File classesDir) {
		String className = classToClassName(klass);
		File classFile = classNameToClassFile(className, classesDir);
		return classFile;
	}

	public static String classToClassName(Class<?> klass) {
		String className = klass.getName();
		return className;
	}
	
	public static String classFileToClassName(File classFile, File classesDir) throws IOException{
        assert(isClassFile(classFile));

        String sClassFile = classFile.getCanonicalPath();
        String sClassesDir = classesDir.getCanonicalPath();

        //bail out if the full class file path doesn't begin
        //with the class directory path
        assert(sClassFile.startsWith(sClassesDir));

        //get everything after the source directory part
        String sPackagedPath = sClassFile.substring(sClassesDir.length() + 1);

        //trim off the '.java' at the end
        int lastKeeperIndex = sPackagedPath.length() - (".class".length()) - 1;
        sPackagedPath = sPackagedPath.substring(0, lastKeeperIndex + 1);

        //replace slashes with '.'
        String sPackagedName = sPackagedPath.replace(File.separatorChar, '.');

        return sPackagedName;		
	}
	
	/**
	 * checks to see if the file is an existing java source code file.
	 */
	public static boolean isSourceFile(File file){
	    if(!file.exists()){
	        return false;
	    }
	    if(!file.isFile()){
	        return false;
	    }
	    if(!(file.toString()).endsWith(".java")){
	        return false;
	    }
	    return true;
	}
	/**
	 * checks to see if the file is an existing java compiled class file.
	 */
	public static boolean isClassFile(File file){
	    if(!file.exists()){
	        return false;
	    }
	    if(!file.isFile()){
	        return false;
	    }
	    if(!(file.toString()).endsWith(".class")){
	        return false;
	    }
	    return true;
	}

}
