package org.meandre.client;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.meandre.client.exceptions.TransmissionException;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.LocationBean;
import org.meandre.core.repository.QueryableRepository;
import org.seasr.meandre.support.generic.io.IOUtils;

import com.hp.hpl.jena.rdf.model.Model;

public abstract class AbstractMeandreClient {

    public abstract void setCredentials(String userName, String password);

    public abstract void setLogger(Logger logger);

    public abstract Logger getLogger();
    
    public abstract String getHostName();
    
    public abstract int getPort();

    public abstract void close();

    /**
     * requests a list of assigned roles of the user (defined by the
     * credentials of this MeandreClient).
     *
     * @return a list of roles
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/about/user_roles.json
     * @throws TransmissionException
     *
     * TODO: Need java object to represent valid roles
     */
    public abstract Set<String> retrieveUserRoles() throws TransmissionException;

    /**
     * requests the list of all valid roles the server supports. the roles
     * are returned in their url form.
     *
     * this is equivalent to getValidRoles() in MeandreAdminClient, but
     * this version requires only the 'about' role and not the 'admin'
     * role to access it. Also, this returns the url representation of
     * the roles, not Role objects.
     *
     * @return list of all valid roles
     */
    public abstract Set<String> retrieveValidRoles() throws TransmissionException;

    /**
     * requests the locations (urls) of all meandre repositories the server
     * is aware of.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/locations/list.json
     */
    public abstract Set<LocationBean> retrieveLocations() throws TransmissionException;

    /**
     * Adds or updates the location of a meandre server peer. returns true
     * if the location is registered with the server after the call (whether
     * it was added or was already present).
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/locations/add.json
     *
     * TODO: Handle possible bad_request errors in http response
     */
    public abstract boolean addLocation(String locationUrl, String description) throws TransmissionException;

    /**
     * removes the input location from the server's list of peers. returns
     * true if the location is not a peer after this method is called
     * (regardless of whether this removed it or if it wasn't there in
     * the first place).
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/locations/remove.json
     */
    public abstract boolean removeLocation(String locationUrl) throws TransmissionException;

    /**
     * Locally recreates a Repository from the RDF model from the server.
     * The contents of the repository are dependent on the user requesting
     * it.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port/services/repository/dump.nt
     * @throws TransmissionException
     */
    public abstract QueryableRepository retrieveRepository() throws TransmissionException;

    /**
     * Tells the server to rebuild it's repository by (re-)querying all
     * of it's peers for information on available components and flows.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/regenerate.json
     */
    public abstract boolean regenerate() throws TransmissionException;

    /**
     * requests the urls of all components in the server repository.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/list_components.json
     * @throws TransmissionException
     */
    public abstract Set<URI> retrieveComponentUris() throws TransmissionException;

    /**
     * requests the urls of all flows in the server repository.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/list_flows.json
     * @throws TransmissionException
     */
    public abstract Set<URI> retrieveFlowUris() throws TransmissionException;

    /**
     * requests all tags for any and all components and flows.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags.json
     * @throws TransmissionException
     *
     * TODO:return tag objects instead of strings
     */
    public abstract Set<String> retrieveAllTags() throws TransmissionException;

    /**
     * requests all tags for all components.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags_components.json
     * @throws TransmissionException
     * TODO:return tag objects instead of strings
     */
    public abstract Set<String> retrieveComponentTags() throws TransmissionException;

    /**
     * requests all tags for all flows.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags_flows.json
     * @throws TransmissionException
     * TODO:return tag objects instead of strings
     */
    public abstract Set<String> retrieveFlowTags() throws TransmissionException;

    /**
     * requests the urls of all components that have the input tag.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/components_by_tag.json
     * @throws TransmissionException
     * TODO:input a tag object instead of string
     */
    public abstract Set<URI> retrieveComponentsByTag(String tag) throws TransmissionException;

    /**
     * requests the urls of all flows that have the input tag.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/flows_by_tag.json
     * @throws TransmissionException
     * TODO:input a tag object instead of string
     */
    public abstract Set<URI> retrieveFlowsByTag(String tag) throws TransmissionException;

    /**
     * requests the component description model from the server.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/describe_component.nt
     * @throws TransmissionException
     */
    public abstract ExecutableComponentDescription retrieveComponentDescriptor(String componentUri) throws TransmissionException;

    /**
     * requests a flow description model from the server.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/describe_flow.nt
     * @throws TransmissionException
     */
    public abstract FlowDescription retrieveFlowDescriptor(String flowUri) throws TransmissionException;

    /**
     *TODO: need serious docs on this or a query object to input instead of
     * a string.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/search_components.json
     * @throws TransmissionException
     */
    public abstract Set<URI> retrieveComponentUrlsByQuery(String query) throws TransmissionException;

    /**
     * TODO: need serious docs on this or a query object to input instead of
     * a string.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/search_flows.json
     * @throws TransmissionException
     */
    public abstract Set<URI> retrieveFlowUrlsByQuery(String query) throws TransmissionException;

    /**
     * uploads a single flow to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     *
     */
    public abstract boolean uploadFlow(FlowDescription flow, boolean overwrite) throws TransmissionException;

    /**
     * uploads a set of flows to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public abstract boolean uploadFlowBatch(Set<FlowDescription> flows, boolean overwrite) throws TransmissionException;

    /**
     * uploads a single component to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     *
     */
    public abstract boolean uploadComponent(ExecutableComponentDescription component, Set<File> jarFileContexts, boolean overwrite)
            throws TransmissionException;

    /**
     * uploads a set of flows to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public abstract boolean uploadComponentBatch(Set<ExecutableComponentDescription> components, Set<File> jarFileContexts, boolean overwrite)
            throws TransmissionException;

    /**
     * uploads all resources of a repository to a server, merging it with
     * the server's repository.
     *
     * the jar files set may be null.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public abstract boolean uploadRepository(QueryableRepository qr, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException;

    /**
     * uploads a set of component or flow resources and any jar files.
     * the jarfiles set may be null.
     *
     * Note: this is the main upload function that actually does the
     * upload. all other upload* methods call this.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public abstract boolean uploadModelBatch(Set<Model> resModels, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException;

    /**
     * Uploads a set of jar files to the resources directory of the server.
     * For instance, jar files required by an applet that a component
     * uses in it's web UI, which are not uploaded with the component itself
     * because the component has no direct dependency on them, would be
     * uploaded via this method and then be available to the applet.
     *
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public abstract boolean uploadFiles(Set<File> files, boolean overwrite) throws TransmissionException;

    /**
     *removes (deletes) either a component or flow from the server. returns
     *true if the resource was deleted.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/remove.json
     * TODO: need more specific error reporting when the server returns an empty
     * json string
     */
    public abstract boolean removeResource(String resourceUri) throws TransmissionException;

    /**
     * commands the server to change a component or flow's status to "published."
     * returns true if the resource is in a state of "published" after this
     * method returns.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/publish/publish.json
     */
    public abstract boolean publish(String resourceUri) throws TransmissionException;

    /**
     * commands the server to change a component or flow's status to
     * "not published."
     *
     * returns true no matter what as long as the server received and understood
     * the request.
     *
     * TODO: modify so returns true if the resource is not in a state of
     * "published" after this method returns.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/publish/unpublish.json
     */
    public abstract boolean unpublish(String resourceUri) throws TransmissionException;

    /**
     * commands the server to run the flow with the given url-name. the returned
     * string is a human readable printout of stdout from the components in the
     * flow and (optionally, if verbose=true) statistics about the flow run.
     *
     * This method currently blocks waiting for flow to complete -> it does
     * not return the result string until the flow has completely finished.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/execute/flow.txt
     */
    public abstract String runFlow(String flowUri, boolean verbose) throws TransmissionException;

    /**
     * This method uploads and executes all the flows in the provided model
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/execute/repository.txt
     * TODO:Need test
     */
    public abstract String runRepository(Model model) throws TransmissionException;

    /**
     * commands the server to run the flow with the given url-name. the returned
     * string is a human readable printout of stdout from the components in the
     * flow and (optionally, if verbose=true) statistics about the flow run.
     *
     * This method currently blocks waiting for flow to complete -> it does
     * not return the result string until the flow has completely finished.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/execute/flow.txt
     */
    public abstract InputStream runFlowStreamOutput(String flowUri, boolean verbose) throws TransmissionException;

    public abstract InputStream runFlowStreamOutput(String flowUri, String token, boolean verbose) throws TransmissionException;

    /**
     * Retrieves the WebUI information for the flow referenced by 'token'
     * Example of WebUI information returned:
     *          port=1716
     *          hostname=192.168.0.2
     *          token=1213938009687
     *          uri=meandre://test.org/flow/webmonkflow/1213938147793/1565344277
     *
     * Note: Not yet unit tested
     *
     * @param token The token of the flow to return the WebUI information for
     * @return  A JSONObject containing the WebUI information
     * @throws TransmissionException
     */
    public abstract JSONObject retrieveWebUIInfo(String token) throws TransmissionException;

    /**
     * returns the job statuses.
     *
     * @return a vector of maps where the keys are status information keys.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/jobs/list_jobs_statuses.json
     *TODO: need to reverse the order in the map so that the always unique
     * webui_url is the key and the not-always-unique flow intance url is
     * the value. requires a server side change.
     * FIXME: This is totally untested.
     */
    public abstract Vector<Map<String, String>> retrieveJobStatuses() throws TransmissionException;

    /**
     * returns the job console.
     *
     * @param sFUID The flow ID
     * @return a string with the console for the given string.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/jobs/job_console.json
     *TODO: need to reverse the order in the map so that the always unique
     * webui_url is the key and the not-always-unique flow intance url is
     * the value. requires a server side change.
     * FIXME: This is totally untested.
     */
    public abstract String retrieveJobConsole(String sFUID) throws TransmissionException;

    /**
     * retrieves the public abstract repository of published resources. does not
     * require authorization.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/public/services/repository.nt
     * @throws TransmissionException
     */
    public abstract QueryableRepository retrievePublicRepository() throws TransmissionException;

    /**
     * retrieves the demo repository of published resources. does not
     * require authorization
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/public/services/demo_repository.nt
     */
    public abstract QueryableRepository retrieveDemoRepository() throws TransmissionException;

    /**
     * commands the WEBUI of a flow abort a running flow. the currently running
     * component will be allowed to complete but no other components in the
     * active flow will fire. the flow is specified by the port on the server that
     * it's webui is running on.
     *
     * if this method returns true, it simply means that the abort
     * request was received by the server, it does not necessarily mean
     * that the currently running component(s) are no longer running.
     *
     *
     *<p> calls:
     *http://<meandre_host>:<webui_port>/admin/abort.txt
     * FIXME: This is totally untested.
     */
    public abstract boolean abortFlow(int webUIPort) throws TransmissionException;
    
    /**
     * requests the current statistics of the currently running flow from
     * the WEBUI. the flow is specified by the port on the server that
     * it's webui is running on.
     *
     * the returned json data is in the format produced by
     * StatisticsProbeImpl.getSerializedStatistics()
     *
     * *<p> calls:
     * http://<meandre_host>:<webui_port>/admin/statistics.json
     *
     * TODO: refactor StatisticsProbeImpl so that a RunningFlowStatistics
     * "bean" can be read and written to/from json, and StatisticsProbeImpl
     * simply constructs the bean.
     * FIXME: This is totally untested.
     */
    public abstract JSONObject retrieveRunningFlowStatisitics(int webUIPort) throws TransmissionException;

    /** 
     * Gets the server version.
     *
     * @return The server version
     * @throws TransmissionException Could not get the server version
     */
    public abstract String getServerVersion() throws TransmissionException;

    /** 
     * Pings the server
     *
     *	@return True if it successfully pinged the server
     */
    public abstract boolean ping() throws TransmissionException;

    public static AbstractMeandreClient getClientForServer(String hostName, int port) {
        try {
            // Check what version of the server we're connecting to
            URL versionUrl = new URL("http", hostName, port, "/services/about/version.json");
            String ver = IOUtils.getTextFromReader(new InputStreamReader(versionUrl.openStream()));
            JSONObject joVersion = new JSONObject(ver);
            
            if (joVersion.getString("version").startsWith("1.4"))
                return new org.meandre.client.v1.MeandreClient(hostName, port);
            
            if (joVersion.getString("version").startsWith("2.0"))
                return new org.meandre.client.v2.MeandreClient(hostName, port);
            
            return null;
        }
        catch (Exception e) {
            // Assume 2.0
            return new org.meandre.client.v2.MeandreClient(hostName, port);
        }
    }
}