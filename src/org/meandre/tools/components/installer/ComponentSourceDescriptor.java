package org.meandre.tools.components.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.meandre.annotations.AnnotationReader;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.Component.Runnable;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentNature;
import org.meandre.annotations.ComponentNatures;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.GenerateComponentDescriptorRdf;
import org.meandre.core.repository.CorruptedDescriptionException;
import org.meandre.core.repository.DataPortDescription;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.PropertiesDescriptionDefinition;
import org.meandre.core.repository.TagsDescription;
import org.meandre.tools.components.installer.util.SourceUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Interface to the annotations of a component class. Also can generate an rdf
 * descriptor file of a component based on the contents of the annotations. The
 * information includes what is essentially what one can get from a
 * org.meandre.core.repository.ExecutableComponentDescription, but comes from
 * the annotations of a class file, not read from an rdf file.
 */

public class ComponentSourceDescriptor {

    /** the Class of the component. */
    final Class<?> _klass;
    final ClassLoader loader;

    /**
     * requires a class that has a Component Annotation.
     * 
     * @throws IllegalArgumentException
     *             if the input class does not have a Component annotation
     */
    public ComponentSourceDescriptor(Class<?> klass) throws IllegalArgumentException {
        if (isClassAComponent(klass)) {
            _klass = klass;
        } else {
            throw (new IllegalArgumentException("The input class \'" + klass.toString() + "\' does not have a Component Annotation."));
        }
        
        loader = klass.getClassLoader();
    }

    /** true if the input klass as a Component Annotation. */
    public static boolean isClassAComponent(Class<?> klass) {
        return (null != klass.getAnnotation(Component.class));
    }

    public String getClassName() {
        return SourceUtil.classToClassName(_klass);
    }

    /**
     * returns the Component Annotation of the class related to this
     * ComponentSourceDescriptor.
     */
    public Component getComponentAnn() {
        return _klass.getAnnotation(Component.class);
    }

    /**
     * get the base url for identifiers related to this component. used for the
     * uri of the component itself and it's location. default is
     * 'http://www.seasrproject.org/components/'
     */
    public String getBaseUrl() {
        String sBaseURL = this.getComponentAnn().baseURL();
        if (sBaseURL.charAt(sBaseURL.length() - 1) != '/') {
            sBaseURL += '/';
        }
        return sBaseURL;
    }

    /**
     * Resource representation of the URI identifier for this type of
     * ExecutableComponent.
     */
    public Resource getExecutableComponentResource() {
        Model model = ModelFactory.createDefaultModel();
        Resource resExecutableComponent = model.createResource(this.getBaseUrl() + this.getCleanComponentName());
        return resExecutableComponent;
    }

    public URI getURI() throws URISyntaxException {
        URI uri = new URI(this.getBaseUrl() + this.getCleanComponentName());
        return uri;
    }

    /**
     * get the name given by the Component Annotation in the component's source
     * file.
     */
    public String getComponentName() {
        return getComponentAnn().name();
    }

    /**
     * get the component description given in the Component Annotation of the
     * component's source file.
     */
    public String getDescription() {
        return this.getComponentAnn().description();
    }

    /**
     * get the license information for this component as a string. From the
     * Component Annotation.
     */
    public String getRights() {
        Licenses primaryRights = this.getComponentAnn().rights();
        switch (primaryRights) {
            case UofINCSA:
                return "University of Illinois/NCSA Open Source License";
            case ASL_2:
                return "Apache License 2.0";
            default:
                String sRightsOther = this.getComponentAnn().rightsOther();
                if (sRightsOther.equals("")) {
                    return "Unknown License";
                } else {
                    return sRightsOther;
                }
        }

    }

    /**
     * the component's creator (author). From the Component Annotation.
     */
    public String getCreator() {
        String sCreator = this.getComponentAnn().creator();
        return sCreator;
    }

    /**
     * gets a Date object representing when this component was created. This is
     * the system time this method is called, not when the component's source
     * was written.
     */
    public Date getCreationDate() {
        Date rightNow = new Date();
        return rightNow;
    }

    /**
     * either 'java', 'python', or 'lisp' as a string. From the Component
     * Annotation.
     */
    public String getRunnable() {
        Runnable runObj = this.getComponentAnn().runnable();
        switch (runObj) {
            case java:
                return "java";
            case python:
                return "python";
            case lisp:
                return "lisp";
            default:
                return "unknown";
        }
    }

    /**
     * returns 'all' or 'any'. From the Component Annotation.
     */
    public String getFiringPolicy() {
        FiringPolicy firingObj = this.getComponentAnn().firingPolicy();
        switch (firingObj) {
            case all:
                return "all";
            case any:
                return "any";
            default:
                return "unknown";
        }
    }

    /**
     * default is "java/class". other possibilites are "jython" and "clojure"
     * 
     */
    public String getFormat() {
        return this.getComponentAnn().format();
    }

    /**
     * the componentName with the whitespace replaced by dashes, all lowercase.
     * the name used in urls.
     * 
     * @return
     */
    public String getCleanComponentName() {
        String sComponentName = this.getComponentName();
        sComponentName = sComponentName.toLowerCase().replaceAll("[ ]+", " ").replaceAll(" ", "-");
        return sComponentName;
    }

    /**
     * base url for the component as a location and it's contexts. form is
     * {baseURL}/{component.name}/implementation/ where component.name is the
     * component name from the annotations with white space removed and all
     * lowercase.
     */
    public String getImplementationBaseUrl() {
        String sBaseURL = this.getBaseUrl();
        String sComponentName = this.getCleanComponentName();
        String sImplUrl = sBaseURL + sComponentName + "/implementation/";
        return sImplUrl;
    }

    /**
     * URL identifier for the location. Form is
     * {baseURL}/{component.name}/implementation/{class.name.with.package}
     */
    public Resource getLocationResource() {
        Model model = ModelFactory.createDefaultModel();
        String sImplBaseUrl = this.getImplementationBaseUrl();
        String sLocation = this.getClassName();
        Resource resLocation = model.createResource(sImplBaseUrl + sLocation);
        return resLocation;
    }

    /**
     * currently returns only the baseImplementationUrl.
     */
    public Set<RDFNode> getContexts() {
        Model model = ModelFactory.createDefaultModel();
        Set<RDFNode> contexts = new HashSet<RDFNode>();
        String sImplUrl = this.getImplementationBaseUrl();
        contexts.add(model.createResource(sImplUrl.trim()));
        return contexts;
    }

    /**
     * get the DataPortDescriptions of the component's inputs. derived from the
     * "ComponentInput" annotations of the component's source file.
     * 
     * @throws CorruptedDescriptionException
     */
    public Set<DataPortDescription> getInputs() throws CorruptedDescriptionException {
        Collection<Annotation> inputAnns = this.getFieldAnnotations("ComponentInput");
        Set<DataPortDescription> dpdInputs = new HashSet<DataPortDescription>();

        String sInputBaseUrl = this.getBaseUrl() + this.getCleanComponentName() + "/input/";
        for (Annotation ann : inputAnns) {
            ComponentInput inputAnn = (ComponentInput) ann;
            String inputName = inputAnn.name();
            String inputDescription = inputAnn.description();
            /*
             * TODO: there should be a constructor in DataPortDescription that
             * takes care of the rest.
             */

            // this comes directly from CreateComponentDescriptor, I'm not
            // really sure what it does -peter
            String sID = inputName.toLowerCase().replaceAll("[ ]+", " ").replaceAll(" ", "-");

            String sInputUrl = sInputBaseUrl + sID;
            Model model = ModelFactory.createDefaultModel();
            Resource inputRes = model.createResource(sInputUrl);

            DataPortDescription inputDpd = new DataPortDescription(inputRes, sInputUrl, inputName, inputDescription);

            dpdInputs.add(inputDpd);
        }
        return dpdInputs;
    }

    /**
     * get the DataPortDescriptions of the component's outputs. derived from the
     * "ComponentOutput" annotations of the component's source file.
     * 
     * @throws CorruptedDescriptionException
     */
    public Set<DataPortDescription> getOutputs() throws CorruptedDescriptionException {
        Collection<Annotation> outputAnns = this.getFieldAnnotations("ComponentOutput");
        Set<DataPortDescription> dpdOutputs = new HashSet<DataPortDescription>();

        String sOutputBaseUrl = this.getBaseUrl() + this.getCleanComponentName() + "/output/";
        for (Annotation ann : outputAnns) {
            ComponentOutput outputAnn = (ComponentOutput) ann;
            String outputName = outputAnn.name();
            String outputDescription = outputAnn.description();
            /*
             * TODO: there should be a constructor in DataPortDescription that
             * takes care of the rest.
             */

            // this comes directly from CreateComponentDescriptor, I'm not
            // really sure what it does -peter
            String sID = outputName.toLowerCase().replaceAll("[ ]+", " ").replaceAll(" ", "-");

            String sOutputUrl = sOutputBaseUrl + sID;
            Model model = ModelFactory.createDefaultModel();
            Resource outputRes = model.createResource(sOutputUrl);

            DataPortDescription outputDpd = new DataPortDescription(outputRes, sOutputUrl, outputName, outputDescription);

            dpdOutputs.add(outputDpd);
        }
        return dpdOutputs;
    }

    /**
     * get the set of Component Properties for the component. From the
     * ComponentProperty annotation of the source file.
     */
    public PropertiesDescriptionDefinition getProperties() {
        Collection<Annotation> propertyAnns = this.getFieldAnnotations("ComponentProperty");

        // {property name -> defaultValue} lookup table
        Hashtable<String, String> htDefaults = new Hashtable<String, String>();

        // {property name -> property description} lookup table
        Hashtable<String, String> htDescriptions = new Hashtable<String, String>();

        for (Annotation ann : propertyAnns) {
            ComponentProperty propertyAnn = (ComponentProperty) ann;
            String name = propertyAnn.name();
            String description = propertyAnn.description();
            String defaultValue = propertyAnn.defaultValue();
            htDefaults.put(name, defaultValue);
            htDescriptions.put(name, description);
        }

        PropertiesDescriptionDefinition pddProperties = new PropertiesDescriptionDefinition(htDefaults, htDescriptions);
        return pddProperties;
    }

    /**
     * gets the list of Tags as a TagsDescription object. From the Component
     * annotation of the source file.
     */
    public TagsDescription getTags() {
        String sTags = this.getComponentAnn().tags();
        sTags = (sTags == null) ? "" : sTags;
        String[] saTmp = new String[0];

        if (!sTags.equals("")) {
            if (sTags.indexOf(',') < 0) {
                saTmp = sTags.toLowerCase().replaceAll("[ ]+", " ").split(" ");
            } else {
                saTmp = sTags.toLowerCase().replaceAll("[ ]+", " ").split(",");
            }
        }
        Set<String> setTmp = new HashSet<String>();
        for (String s : saTmp) {
            setTmp.add(s.trim());
        }
        TagsDescription tagDesc = new TagsDescription(setTmp);
        return tagDesc;
    }

    /**
     * gets the component's mode (webui or compute) identifier as a Resource.
     * from the Component Annotation in the source file.
     */
    public Resource getModeResource() {
        Mode mode = this.getComponentAnn().mode();
        switch (mode) {
            case webui:
                return ExecutableComponentDescription.WEBUI_COMPONENT;
            case compute:
                return ExecutableComponentDescription.COMPUTE_COMPONENT;
            default:
                return ExecutableComponentDescription.COMPUTE_COMPONENT;
        }

    }

    /**
     * this method currently does not use it's internal values from the 'get*'
     * methods, but rather delegates construction of the
     * ExecutableComponentDescription to the CreateDefaultComponentDescriptor
     * class.
     * 
     * @return
     * @throws CorruptedDescriptionException
     * @throws ClassNotFoundException
     */
    public ExecutableComponentDescription toExecutableComponentDescription() throws CorruptedDescriptionException,
            ClassNotFoundException {

        AnnotationReader ar = new AnnotationReader();
        ar.findAnnotations(SourceUtil.classToClassName(_klass), loader);

        GenerateComponentDescriptorRdf comDescRdf = new GenerateComponentDescriptorRdf();
        comDescRdf.setComponentInputKeyValues(ar.getComponentInputKeyValues());
        comDescRdf.setComponentOutputKeyValues(ar.getComponentOutputKeyValues());
        comDescRdf.setComponentPropertyKeyValues(ar.getComponentPropertyKeyValues());
        comDescRdf.setComponentKeyValues(ar.getComponentKeyValues());

        ExecutableComponentDescription ecd = comDescRdf.getExecutableComponentDescription();

        return ecd;
        /*
         * ExecutableComponentDescription ecd = new
         * ExecutableComponentDescription(
         * this.getExecutableComponentResource(), this.getComponentName(),
         * this.getDescription(), this.getRights(), this.getCreator(),
         * this.getCreationDate(), this.getRunnable(), this.getFiringPolicy(),
         * this.getFormat(), this.getContexts(), this.getLocationResource(),
         * this.getInputs(), this.getOutputs(), this.getProperties(),
         * this.getTags(), this.getModeResource() ); return ecd;
         */
    }

    public File getRdfDestinationForDir(File outputDir) {
        String sBaseName = this.getClassName() + ".rdf";
        File rdfFile = new File(outputDir, sBaseName);
        return rdfFile;
    }

    /**
     * writes the rdf to disk and returns the file that is written to in the
     * outputDir.
     * 
     * @throws IOException
     *             if the destination file can't be written.
     * @throws CorruptedDescriptionException
     * @throws ClassNotFoundException
     */
    public File writeRDFToDir(File outputDir) throws IOException, CorruptedDescriptionException, ClassNotFoundException {
        File outputFile = this.getRdfDestinationForDir(outputDir);
        FileOutputStream fos = new FileOutputStream(outputFile);
        ExecutableComponentDescription ecd = this.toExecutableComponentDescription();
        ecd.getModel().write(fos);
        fos.close();
        return outputFile;
    }

    /**
     * gets all instances of annotations on fields (of the class) that have a
     * name ending in annotationNameSuffix.
     * 
     * @param annotationNameSuffix
     *            known supported values are "ComponentOutput" "ComponentInput"
     *            and "ComponentProperty"
     * @return
     */
    private Collection<Annotation> getFieldAnnotations(String annotationNameSuffix) {

        // collect any annotations that have the suffix in this set
        Collection<Annotation> foundAnnotations = new ArrayList<Annotation>();

        Field fields[] = _klass.getDeclaredFields();
        for (Field fieldOfClass : fields) {
            Annotation[] localAnnotations = fieldOfClass.getAnnotations();
            for (Annotation ann : localAnnotations) {
                String annName = ann.annotationType().getCanonicalName();
                if (annName.endsWith(annotationNameSuffix)) {
                    foundAnnotations.add(ann);
                }
            }
        }
        return foundAnnotations;
    }

    /**
     * gets the basenames of the jar files declared as dependencies. This is the
     * contents of the "dependencies()" field of the Component Annoation.
     */
    public Set<String> getDeclaredJarDependencies() {
        Set<String> strs = new HashSet<String>();
        for (String s : this.getComponentAnn().dependency()) {
            // the default is to have a single empty string, do not include it
            if (!s.equals("")) {
                strs.add(s);
            }
        }
        return strs;
    }

    /**
     * like getJarNames(), but returns the 'resources' file base names. from
     * "resources" of the component Annotation.
     */
    public Set<String> getDeclaredFileResources() {
        Set<String> strs = new HashSet<String>();
        for (String s : this.getComponentAnn().resources()) {
            // the default is to have a single empty string, do not include it
            if (!s.equals("")) {
                strs.add(s);
            }
        }
        return strs;
    }

    /**
     * gets all ComponentNatures Annotations as a set. If a ComponentNature
     * annotation was present in the class, then a set with just that annotation
     * will be returned. If a ComponentNatures Ann was used, all will be
     * returned. If no ComponentNature(s) tag was present, the returned set will
     * be empty.
     */
    private Set<ComponentNature> getComponentNatureAnns() {
        Set<ComponentNature> setCompNature = new HashSet<ComponentNature>(3);

        Annotation compNature = _klass.getAnnotation(ComponentNature.class);
        if (null != compNature) {
            setCompNature.add((ComponentNature) compNature);
        }

        Annotation compNatures = _klass.getAnnotation(ComponentNatures.class);
        if (null != compNatures) {
            ComponentNature[] aryNature = ((ComponentNatures) compNatures).natures();
            for (ComponentNature cn : aryNature) {
                setCompNature.add(cn);
            }
        }
        return setCompNature;
    }

    public boolean hasApplet() {
        boolean hasApplet = false;
        Set<ComponentNature> setCompNature = this.getComponentNatureAnns();
        for (ComponentNature cn : setCompNature) {
            if (cn.type().equals("applet")) {
                hasApplet = true;
            }
        }
        return hasApplet;
    }

    public Set<String> getAppletClassNames() {
        Set<String> appletClassNames = new HashSet<String>(5);
        Set<ComponentNature> setCompNature = this.getComponentNatureAnns();
        for (ComponentNature cn : setCompNature) {
            if (cn.type().equals("applet")) {
                appletClassNames.add(cn.extClass().getCanonicalName());
            }
        }
        return appletClassNames;
    }

    /**
     * for each ComponentNature of the class, creates an entry in the map
     * between the applet class name (the extClass() field of the annotation)
     * and the jar dependencies (the dependency() field of the annotation).
     */
    private Map<String, Set<String>> getAppletNameToAppletDependenciesMap() {
        // the map to return
        Map<String, Set<String>> jarDepMap = new HashMap<String, Set<String>>(5);

        // iterate over all the componentNature's adding the entries
        Set<ComponentNature> setCompNature = this.getComponentNatureAnns();
        for (ComponentNature cn : setCompNature) {
            if (cn.type().equals("applet")) {
                String appletClassName = (cn.extClass().getCanonicalName());
                Set<String> deps = new HashSet<String>(5);
                for (String dep : cn.dependency()) {
                    deps.add(dep);
                }
                jarDepMap.put(appletClassName, deps);
            }
        }
        return jarDepMap;
    }

    /**
     * for each ComponentNature of the class, creates an entry in the map
     * between the applet class name (the extClass() field of the annotation)
     * and the file dependencies (the resources() field of the annotation).
     */
    private Map<String, Set<String>> getAppletNameToAppletResourcesMap() {
        // the map to return
        Map<String, Set<String>> resDepMap = new HashMap<String, Set<String>>(5);

        // iterate over all the componentNature's adding the entries
        Set<ComponentNature> setCompNature = this.getComponentNatureAnns();
        for (ComponentNature cn : setCompNature) {
            if (cn.type().equals("applet")) {
                String appletClassName = (cn.extClass().getCanonicalName());
                Set<String> deps = new HashSet<String>(5);
                for (String dep : cn.resources()) {
                    deps.add(dep);
                }
                resDepMap.put(appletClassName, deps);
            }
        }
        return resDepMap;
    }

    /**
     * gets the list of jar file dependencies (the base filenames) associated
     * with the appletClassName. Works for all values returned by
     * getAppletClassName(). These are the jar files declared in the
     * "dependencies" field of the ComponentNature.
     */
    public Set<String> getDeclaredAppletJarDependencies(String appletClassName) {
        return this.getAppletNameToAppletDependenciesMap().get(appletClassName);
    }

    /**
     * gets the list of file dependencies (the base filenames) associated with
     * the appletClassName. Works for all values returned by
     * getAppletClassName(). These are the non-jar dependencies declared in the
     * "resources" field of the ComponentNature.
     */
    public Set<String> getDeclaredAppletFileDependencies(String appletClassName) {
        return this.getAppletNameToAppletResourcesMap().get(appletClassName);
    }

}
