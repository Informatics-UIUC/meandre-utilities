package org.meandre.tools.components.installer;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.meandre.tools.components.installer.util.DependencyAnalyzer;
import org.meandre.tools.components.installer.util.FileUtil;
import org.meandre.tools.components.installer.util.JarUtil;
import org.meandre.tools.components.installer.util.SourceUtil;

public class ComponentJarBuilder {

    File _classDir;
    File _outputDir;
    DependencyAnalyzer _depFinder;
    
    public ComponentJarBuilder(File classesDir, File jarOutputDir,
            DependencyAnalyzer dependencyFinder) {
        
        _classDir = classesDir;
        _outputDir = jarOutputDir;
        _depFinder = dependencyFinder;
    }

    /**
     * creates a File object for the Jar file that would be built by the
     * writeJar() method.
     * 
     * @param canonicalClassName
     * @return
     */
    public File getJarFileLocator(String canonicalClassName){
        String fileBaseName = canonicalClassName + ".jar";
        File jarFile = new File(_outputDir, fileBaseName);
        return jarFile;
    }
    
    /**
     * creates a jar file (on disk) for the component described by the
     * input descriptor. The name of the file will be the value of
     * getJarFileLocator(compDescriptor.getClassName()).
     * 
     * @param compDescriptor
     * @return the File that was written to disk. 
     * @throws IOException 
     */
    public File writeJar(ComponentSourceDescriptor compDescriptor) 
            throws IOException {
        String className = compDescriptor.getClassName();
        File compClassFile = 
                SourceUtil.classNameToClassFile(className, _classDir);
        
        //include the classFiles that the component class depends on
        //from the classes directory
        Set<File> includeFiles = _depFinder.getDeepClassDeps(compClassFile);
        
        //include the other file resources required by the component
        Set<String> ssResource = compDescriptor.getDeclaredFileResources();
        Set<File> fileResources = FileUtil.findFilesInDirectory(
                ssResource, _classDir);
        includeFiles.addAll(fileResources);
        
        //build the jar file
        File jarFile = getJarFileLocator(className);
        try{
            JarUtil.createJarFile(jarFile);
            JarUtil.setManifest(jarFile, getDefaultManifest());
            JarUtil.addFilesToJarFile(jarFile, includeFiles, _classDir);
        }catch(IOException e){
            //in case of a problem, delete the jar file so that a half done
            //jar isn't left behind
            jarFile.delete();
            throw e;
        }
        
        return jarFile;
    }

    public static Manifest getDefaultManifest() {
        Manifest manifest = new Manifest();
        Attributes attMap = manifest.getMainAttributes();
        attMap.putValue("Manifest-Version", "1.0");
		attMap.putValue("isComponent", "true");
        return manifest;
    }

    
    private void log(String msg){
        System.out.println("ComponentJarBuilder: " + msg);
    }
}
