/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.meandre.tools.zigzag.transformations;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.meandre.core.repository.ConnectorDescription;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.ExecutableComponentInstanceDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.tools.Version;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Class that generates a ZigZag script based on a flow
 *
 * Dependencies:
 *
 * jena-2.6.3.jar
 * meandre-repository-1.4.9.jar
 *
 * @author Boris Capitanu
 */
public class RDF2ZZConverter {
    private static final String NEWLINE = System.getProperty("line.separator");

    private final QueryableRepository _repository;
    private String _importSource;

    /**
     * Constructor
     *
     * @param repository The repository containing the flow(s) you want to convert
     */
    public RDF2ZZConverter(QueryableRepository repository) {
        _repository = repository;
    }

    /**
     * Sets the location used for importing components
     *
     * @param importSource The URL to the file containing the component descriptors
     */
    public void setImportSource(String importSource) {
        _importSource = importSource;
    }

    /**
     * Generates a ZigZag script for the specified flow
     *
     * @param flowURI The flow URI
     * @return The ZigZag script
     * @throws FlowNotFoundException Thrown if the specified flow couldn't be found in the given repository
     */
    public String generateZZ(String flowURI) throws FlowNotFoundException {
        Map<String, FlowDescription> flows = _repository.getAvailableFlowDescriptionsMap();
        FlowDescription flow = flows.get(flowURI);

        if (flow == null) throw new FlowNotFoundException(flowURI);

        Map<Resource, String> aliasMap = createAliasMap(flow);
        Map<ExecutableComponentInstanceDescription, String> instantiationMap = createInstantiationMap(flow);

        StringBuilder zzScript = new StringBuilder();
        zzScript.append(generateZZHeader(flow)).append(NEWLINE);
        zzScript.append(generateZZImports()).append(NEWLINE);
        zzScript.append(generateZZAliases(aliasMap)).append(NEWLINE);
        zzScript.append(generateZZInstantiations(instantiationMap, aliasMap)).append(NEWLINE);
        zzScript.append(generateZZPropertyAssignments(instantiationMap));
        zzScript.append(generateZZConnections(flow, instantiationMap)).append(NEWLINE);
        zzScript.append(generateZZFooter(flow));

        return zzScript.toString();
    }

    /**
     * Generates the header for the ZigZag script
     *
     * @param flow The flow descriptor object
     * @return The ZigZag header
     */
    protected String generateZZHeader(FlowDescription flow) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# ").append(String.format("Generated by %s v%s on %tc", getClass().getSimpleName(), Version.getFullVersion(), new Date())).append(NEWLINE);
        sb.append("#").append(NEWLINE);
        sb.append(String.format("# @name \t%s", flow.getName())).append(NEWLINE);
        sb.append(String.format("# @description \t%s", flow.getDescription().trim().replaceAll("\n", "\n#              \t"))).append(NEWLINE);
        sb.append(String.format("# @creator \t%s", flow.getCreator())).append(NEWLINE);
        sb.append(String.format("# @date \t%tc", flow.getCreationDate())).append(NEWLINE);
        sb.append(String.format("# @rights \t%s", flow.getRights().trim().replaceAll("\n", "\n#         \t"))).append(NEWLINE);
        sb.append(String.format("# @tags \t%s", flow.getTags().toString())).append(NEWLINE);
        sb.append(String.format("# @uri  \t%s", flow.getFlowComponentAsString())).append(NEWLINE);
        sb.append("#").append(NEWLINE);

        return sb.toString();
    }

    /**
     * Generates the footer for the ZigZag script
     *
     * @param flow The flow descriptor object
     * @return The ZigZag footer
     */
    protected String generateZZFooter(FlowDescription flow) {
        return "";
    }

    /**
     * Generates the 'import' statement for the ZigZag script
     *
     * @return The ZigZag import section
     */
    protected String generateZZImports() {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# Specify component imports").append(NEWLINE);
        sb.append("#").append(NEWLINE);

        if (_importSource == null) {
            sb.append("# TODO: Add component import statement(s) here").append(NEWLINE);
            sb.append("# Example: import <URL>   (replace 'URL' with the correct location)").append(NEWLINE);
        } else
            sb.append(String.format("import <%s>", _importSource)).append(NEWLINE);

        return sb.toString();
    }

    /**
     * Generates the 'alias' statement(s) for the ZigZag script
     *
     * @param aliasMap The component to alias mapping
     * @return The ZigZag alias section
     */
    protected String generateZZAliases(Map<Resource, String> aliasMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# Create the component aliases").append(NEWLINE);
        sb.append("#").append(NEWLINE);

        for (Entry<Resource, String> alias : aliasMap.entrySet())
            sb.append(String.format("alias <%s> as %s", alias.getKey().getURI(), alias.getValue())).append(NEWLINE);

        return sb.toString();
    }

    /**
     * Generates the component instantiations for the ZigZag script
     *
     * @param instantiationMap The component instance to ZigZag instance name mapping
     * @param aliasMap The component to ZigZag alias mapping
     * @return The ZigZag component instantiations section
     */
    protected String generateZZInstantiations(
            Map<ExecutableComponentInstanceDescription, String> instantiationMap,
            Map<Resource, String> aliasMap) {

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# Create the component instances").append(NEWLINE);
        sb.append("#").append(NEWLINE);

        for (Entry<ExecutableComponentInstanceDescription, String> instance : instantiationMap.entrySet()) {
            Resource resComponent = instance.getKey().getExecutableComponent();
            sb.append(String.format("%s = %s()", instance.getValue(), aliasMap.get(resComponent))).append(NEWLINE);
        }

        return sb.toString();
    }

    /**
     * Generates the component property assignments for the ZigZag script
     *
     * @param instantiationMap The component instance to ZigZag instance name mapping
     * @return The ZigZag component property assignments section
     */
    protected String generateZZPropertyAssignments(Map<ExecutableComponentInstanceDescription, String> instantiationMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# Set component properties").append(NEWLINE);
        sb.append("#").append(NEWLINE);

        for (Entry<ExecutableComponentInstanceDescription, String> instance : instantiationMap.entrySet()) {
            String compInstanceVarName = instance.getValue();

            ExecutableComponentInstanceDescription compInstance = instance.getKey();
            Map<String, String> compInstanceProps = compInstance.getProperties().getValueMap();

            ExecutableComponentDescription compDesc =
                _repository.getExecutableComponentDescription(compInstance.getExecutableComponent());
            Map<String, String> compDefaultProps = compDesc.getProperties().getValueMap();

            for (Entry<String, String> property : compInstanceProps.entrySet())
                if (compDefaultProps.containsKey(property.getKey()))
                    sb.append(String.format("%s.%s = \"%s\"", compInstanceVarName,
                        property.getKey(), property.getValue().replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\""))).append(NEWLINE);

            sb.append(NEWLINE);
        }

        return sb.toString();
    }

    /**
     * Generates the component connections for the ZigZag script
     *
     * @param flow The flow descriptor object
     * @param instantiationMap The component instance to ZigZag instance name mapping
     * @return The ZigZag component connections section
     */
    protected String generateZZConnections(FlowDescription flow, Map<ExecutableComponentInstanceDescription, String> instantiationMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(NEWLINE);
        sb.append("# Create the flow by connecting the components").append(NEWLINE);
        sb.append("#").append(NEWLINE);

        Map<String, String> connectionsMap = new HashMap<String, String>();
        Set<String> sourceComponents = new HashSet<String>();

        for (ConnectorDescription connector : flow.getConnectorDescriptions()) {
            ExecutableComponentInstanceDescription srcInstance = flow.getExecutableComponentInstanceDescription(connector.getSourceInstance());
            ExecutableComponentInstanceDescription targetInstance = flow.getExecutableComponentInstanceDescription(connector.getTargetInstance());

            Resource resSrcPort = connector.getSourceInstanceDataPort();
            Resource resTargetPort = connector.getTargetInstanceDataPort();

            Resource resSrcComponent = srcInstance.getExecutableComponent();
            Resource resTargetComponent = targetInstance.getExecutableComponent();

            ExecutableComponentDescription srcComp = _repository.getExecutableComponentDescription(resSrcComponent);
            ExecutableComponentDescription targetComp = _repository.getExecutableComponentDescription(resTargetComponent);

            String srcPortName = (srcComp != null) ? srcComp.getOutput(resSrcPort).getName() : getLastPart(resSrcPort.getURI());
            String targetPortName = (targetComp != null) ? targetComp.getInput(resTargetPort).getName() : getLastPart(resTargetPort.getURI());

            String srcInstanceName = instantiationMap.get(srcInstance);
            String targetInstanceName = instantiationMap.get(targetInstance);

            // Sanity check
            if (srcPortName.contains(" "))
                throw new RuntimeException("Port names cannot contain spaces! component: " + srcInstance.getName() + " port: " + srcPortName);
            if (targetPortName.contains(" "))
                throw new RuntimeException("Port names cannot contain spaces! component: " + targetInstance.getName() + " port: " + targetPortName);

            if (srcComp == null)
                sb.append(String.format("# WARNING: Component name '%s' of type '%s' with alias '%s_outputs':%n#\tGuessing name of" +
                        " output port with id '%s' as '%s'! Change if incorrect!",
                        srcInstance.getName(), resSrcComponent.getURI(), srcInstanceName, resSrcPort.getURI(), srcPortName)).append(NEWLINE);
            if (targetComp == null)
                sb.append(String.format("# WARNING: Component name '%s' of type '%s' with alias '%s':%n#\tGuessing name of" +
                        " input port with id '%s' as '%s'! Change if incorrect!",
                        targetInstance.getName(), resTargetComponent.getURI(), targetInstanceName, resTargetPort.getURI(), targetPortName)).append(NEWLINE);

            String connections = null;

            if ((connections = connectionsMap.get(targetInstanceName)) != null)
                connections += "; ";
            else
                connections = "";

            connections += String.format("%s: %s_outputs.%s", targetPortName, srcInstanceName, srcPortName);
            connectionsMap.put(targetInstanceName, connections);
            sourceComponents.add(srcInstanceName);
        }

        for (String instanceName : sourceComponents)
            sb.append(String.format("@%1$s_outputs = %1$s()", instanceName)).append(NEWLINE);

        sb.append(NEWLINE);

        for (Entry<String, String> entry : connectionsMap.entrySet()) {
            String[] portAssignments = entry.getValue().split(";");
            sb.append(String.format("%s(", entry.getKey()));

            // change the >N to specify when wrapping should occur: wrap only if there more than N port assignments in this statement
            if (portAssignments.length > 1) {
                sb.append(NEWLINE);
                for (int i = 0; i < portAssignments.length; i++)
                    sb.append("\t" + portAssignments[i].trim()).append((i == portAssignments.length-1) ? "" : ";").append(NEWLINE);
                sb.append(")").append(NEWLINE);
            }
            else
                sb.append(String.format("%s)", entry.getValue())).append(NEWLINE);
        }

        return sb.toString();
    }

    /**
     * Creates the ZigZag aliases for the components used by the flow
     *
     * @param flow The flow descriptor object
     * @return A mapping from components to alias names
     */
    protected Map<Resource, String> createAliasMap(FlowDescription flow) {
        Map<Resource, String> zzAliases = new HashMap<Resource, String>();

        for (ExecutableComponentInstanceDescription instance : flow.getExecutableComponentInstances()) {
            Resource resComponent = instance.getExecutableComponent();
            ExecutableComponentDescription component = _repository.getExecutableComponentDescription(resComponent);
            String componentAlias = null;

            if ((componentAlias = zzAliases.get(resComponent)) == null) {
                componentAlias = ((component != null) ? component.getName().replaceAll(" |\t", "_") : getLastPart(resComponent.getURI())).toUpperCase();
                zzAliases.put(resComponent, componentAlias);
            }
        }

        return zzAliases;
    }

    /**
     * Creates the ZigZag component instantiations
     *
     * @param flow The flow descriptor object
     * @return A mapping from component instances to ZigZag instance names
     */
    protected Map<ExecutableComponentInstanceDescription, String> createInstantiationMap(FlowDescription flow) {
        Map<ExecutableComponentInstanceDescription, String> zzInstances = new HashMap<ExecutableComponentInstanceDescription, String>();
        Map<ExecutableComponentDescription, Integer> zzInstanceCounter = new HashMap<ExecutableComponentDescription, Integer>();

        for (ExecutableComponentInstanceDescription instance : flow.getExecutableComponentInstances()) {
            ExecutableComponentDescription component = _repository.getExecutableComponentDescription(instance.getExecutableComponent());
            String compInstanceName = instance.getName().toLowerCase().replaceAll(" |\t", "_");
            Integer counter = null;

            if ((counter = zzInstanceCounter.get(component)) != null && zzInstances.containsValue(compInstanceName))
                compInstanceName += "_" + ++counter;
            else
                counter = 1;

            zzInstances.put(instance, compInstanceName);
            zzInstanceCounter.put(component, counter);
        }

        return zzInstances;
    }

    /**
     * Retrieves the last portion of a URI
     *
     * @param uri The URI
     * @return The last segment of the URI
     */
    private String getLastPart(String uri) {
        String result = null;

        String[] parts = uri.split("/");
        for (int i = parts.length-1; i >= 0; i--)
            if (parts[i].length() > 0) {
                result = parts[i];
                break;
            }

        return result;
    }
}
