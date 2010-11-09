/**
 * 
 */
package org.meandre.tools.components.installer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.meandre.tools.components.installer.util.DependencyAnalyzer;
import org.meandre.tools.components.installer.util.JarUtil;
import org.meandre.tools.components.installer.util.SourceUtil;

/**
 * @author pgroves
 *
 */
public class AppletJarBuilder {

    File _classDir;
    File _outputDir;
    DependencyAnalyzer _depFinder;

    public AppletJarBuilder(File classesDir, File jarOutputDir,
            DependencyAnalyzer dependencyFinder) {
        
        _classDir = classesDir;
        _outputDir = jarOutputDir;
        _depFinder = dependencyFinder;
    }
    
    /**
     * creates a File object for the Jar file that would be built by the
     * writeJar() method.
     * 
     * @param canonicalClassName the full classname (with package)
     * @return
     */
    public File getJarFileLocator(String canonicalClassName){
        String fileBaseName = canonicalClassName + ".jar";
        fileBaseName = fileBaseName.toLowerCase();
        File jarFile = new File(_outputDir, fileBaseName);
        return jarFile;
    }

    /**
     * creates a jar file (on disk) for the component described by the
     * input descriptor. The name of the file will be the value of
     * getJarFileLocator(compDescriptor.getClassName()).
     * 
     * @param appletClassName the canonical name of the the applet class
     * @param resourceFiles a set of files to include in the jar that are not
     * expected to be picked up by the dependency analyzer. The files must be
     * in this AppletJarBuiler's class directory.
     * @return the File that was written to disk. 
     * @throws IOException 
     */
    public File writeJar(String appletClassName, Set<File> resourceFiles) 
            throws IOException {
        File appletClassFile = 
                SourceUtil.classNameToClassFile(appletClassName, _classDir);
        
        //include the classFiles that the applet class depends on
        //from the classes directory
        Set<File> includeFiles = _depFinder.getDeepClassDeps(appletClassFile);
        
        //include the other file resources required by the component
        includeFiles.addAll(resourceFiles);
        
        //build the jar file
        File jarFile = getJarFileLocator(appletClassName);
        try{
            JarUtil.createJarFile(jarFile);
            JarUtil.addFilesToJarFile(jarFile, includeFiles, _classDir);
        }catch(IOException e){
            //in case of a problem, delete the jar file so that a half done
            //jar isn't left behind
            jarFile.delete();
            throw e;
        }
        return jarFile;
    }
}
