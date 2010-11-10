package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentNature;
import org.meandre.annotations.ComponentNatures;

/**
 * convenience functions for reading meandre specific annotation information
 * from source files and compiled .class files.
 *
 * @author Peter Groves
 */

public class AnnotationUtil{

    /**
     * searches a source directory for any .java files containing
     * a class with an @Component annotation
     *
     * @return the set of .java files in the srcDir that have @Component tags
     */
    public static Set<File> findComponentSourceFiles(File srcDir, ClassLoader loader)
    		throws IOException, ClassNotFoundException{
        Set<File> foundCompFiles = new HashSet<File>();

        Iterator<File> iter = new FileTreeIterator(srcDir);
        while(iter.hasNext()){
        	File nxtFile = iter.next();
            if(sourceFileHasComponentTag(nxtFile, srcDir, loader)){
                foundCompFiles.add(nxtFile);
            }
        }

        return foundCompFiles;
    }
    /**
     * helper method for findComponentSourceFiles to check if
     * a source file represents a class with a component annotation.
     */
    private static boolean sourceFileHasComponentTag(File srcFile,
    		File srcDir, ClassLoader loader) throws IOException, ClassNotFoundException{
        if(!SourceUtil.isSourceFile(srcFile)){
            return false;
        }
        Class<?> klass = SourceUtil.sourceFileToClass(srcFile, srcDir, loader);
        boolean hasTag = classHasComponentTag(klass);
        return hasTag;

    }

    /**
     * checks if the input Class<?> has a Component annotation. 
     */
    public static boolean classHasComponentTag(Class<?> klass){
        Class<? extends Annotation> annotClass = Component.class;
        return klass.isAnnotationPresent(annotClass);
    }

    /**
     * retrieves the Component annotation of the input class, or
     * null if the class does not have it.
     */
    public static Component getComponentOfClass(Class<?> klass){
        Class<? extends Annotation> annotationClass = Component.class;
        Component compAnnot = (Component)klass.getAnnotation(annotationClass);
        return compAnnot;
    }

    /**
     * checks if the input Class<?> has a ComponentNature annotation. 
     */
    public static boolean classHasComponentNatureTag(Class<?> klass){
        Class<? extends Annotation> annotClass = ComponentNature.class;
        return klass.isAnnotationPresent(annotClass);
        
    }
    /**
     * retrieves the ComponentNature annotation of the input class, or
     * null if the class does not have it.
     */
    public static ComponentNature getComponentNatureOfClass(
            Class<?> componentKlass){

        Class<? extends Annotation> annotationClass = ComponentNature.class;
        ComponentNature natureAnnot = 
        		(ComponentNature)componentKlass.getAnnotation(annotationClass);
        return natureAnnot;
    }

    /**
     * checks if the input Class<?> has a ComponentNatures annotation. 
     */
    public static boolean classHasComponentNaturesTag(Class<?> klass){
        Class<? extends Annotation> annotClass = ComponentNatures.class;
        return klass.isAnnotationPresent(annotClass);
        
    }

    /**
     * retrieves the ComponentNatures annotation of the input class, or
     * null if the class does not have it.
     */
    public static ComponentNatures getComponentNaturesOfClass(
            Class<?> componentKlass){

        Class<? extends Annotation> annotationClass = ComponentNatures.class;
        ComponentNatures naturesAnnot = 
        	(ComponentNatures)componentKlass.getAnnotation(annotationClass);
        return naturesAnnot;

    }


}
