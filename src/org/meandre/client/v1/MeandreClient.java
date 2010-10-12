package org.meandre.client.v1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.meandre.client.utils.ClientLoggerFactory;
import org.meandre.client.utils.GenericHttpClient;
import org.meandre.client.utils.KeyValuePair;
import org.meandre.client.utils.TransmissionException;
import org.meandre.client.utils.handlers.JSONResponseHandler;
import org.meandre.client.utils.handlers.RDFModelResponseHandler;
import org.meandre.client.utils.handlers.StreamResponseHandler;
import org.meandre.client.utils.handlers.StringResponseHandler;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.LocationBean;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


/**
 * Programmatic interface to the Meandre server webservices API. Mimicks opening
 * a session with the server and allowing the client to interact with it,
 * although in reality the session has no state and MeandreClient simply sends an
 * independent http request for every operation.
 *
 * <p>The setCredentials() method must be called before authorized calls on
 * a MeandreClient can be invoked. All calls are authorized unless they say
 * specifically that they do not require authorization.
 *
 * @author Boris Capitanu
 */
public class MeandreClient {

    private final GenericHttpClient _httpClient;

    /**
     * initialize to talk to a particular server. You need to call the
     * "setCredentials" method in MeandreBaseClient before you can make
     * authorized calls to the server.
     *
     *
     * @param serverHost just the hostname, e.g. "localhost",
     *      NOT "http://localhost"
     * @param port the port on the serverHost that the server is listening on
     */
    public MeandreClient(String serverHost, int port) {
        _httpClient = new GenericHttpClient(serverHost, port, ClientLoggerFactory.getClientLogger());
    }

    public void setCredentials(String userName, String password) {
        _httpClient.setCredentials(userName, password);
    }
    
    public void setLogger(Logger logger) {
        _httpClient.setLogger(logger);
    }
    
    public Logger getLogger() {
        return _httpClient.getLogger();
    }
    
    public void close() {
        _httpClient.close();
    }

    /////////
    //About
    /////////

    /**
     * requests all java properties of the server's Store.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/about/installation.rdf
     *
     * TODO: need a java object instance to represent installation properties
     */
    public JSONObject retrieveInstallationProperties() throws TransmissionException {
        String reqPath = "/services/about/installation.json";
        JSONTokener jt = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return (new JSONArray(jt)).getJSONObject(0);
        } 
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

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
    public Set<String> retrieveUserRoles() throws TransmissionException {
        String reqPath = "/services/about/user_roles.json";
        JSONTokener jtRoles = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray ja = new JSONArray(jtRoles);

            Set<String> roles = new HashSet<String>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                roles.add(ja.getJSONObject(i).getString("meandre_role_uri"));

            return roles;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

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
    public Set<String> retrieveValidRoles() throws TransmissionException {
        String reqPath = "/services/about/valid_roles.json";
        JSONTokener jtRoles = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray ja = new JSONArray(jtRoles);
            
            Set<String> roles = new HashSet<String>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                roles.add(ja.getJSONObject(i).getString("meandre_role_uri"));
            
            return roles;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

    /////////
    //Locations (known peers of the server)
    //////////

    /**
     * requests the locations (urls) of all meandre repositories the server
     * is aware of.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/locations/list.json
     */
    public Set<LocationBean> retrieveLocations() throws TransmissionException {
        String reqPath = "/services/locations/list.json";
        JSONTokener jtLocs = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtLocs);
            
            HashSet<LocationBean> beanSet = new HashSet<LocationBean>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++) {
                JSONObject jo = ja.getJSONObject(i);
                beanSet.add(new LocationBean(jo.getString("location"), jo.getString("description")));
            }

            return beanSet;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

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
    public boolean addLocation(String locationUrl, String description) throws TransmissionException {
        String reqPath = "/services/locations/add.json";

        NameValuePair[] nvps = new BasicNameValuePair[2];
        nvps[0] = new BasicNameValuePair("location", locationUrl);
        nvps[1] = new BasicNameValuePair("description", description);

        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), nvps);
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            JSONObject joRetrieved = ja.getJSONObject(0);
            
            String loc = joRetrieved.getString("location");
            String descr = joRetrieved.getString("description");
            
            return loc.equals(locationUrl) && descr.equals(description);
        }
        catch (JSONException e) {
            return false;
        }
    }

    /**
     * removes the input location from the server's list of peers. returns
     * true if the location is not a peer after this method is called
     * (regardless of whether this removed it or if it wasn't there in
     * the first place).
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/locations/remove.json
     */
    public boolean removeLocation(String locationUrl) throws TransmissionException {
        String reqPath = "/services/locations/remove.json";

        NameValuePair argLoc = new BasicNameValuePair("location", locationUrl);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argLoc);
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            String location = ja.getJSONObject(0).getString("location");
            
            return location.equals(locationUrl);
        }
        catch (JSONException e) {
            return false;
        }
    }

    ////////////
    //Repository
    /////////////

    /**
     * Locally recreates a Repository from the RDF model from the server.
     * The contents of the repository are dependent on the user requesting
     * it.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port/services/repository/dump.nt
     * @throws TransmissionException
     */
    public QueryableRepository retrieveRepository() throws TransmissionException {
        String reqPath = "/services/repository/dump.nt";
        Model model = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(model);
    }

    /**
     * Tells the server to rebuild it's repository by (re-)querying all
     * of it's peers for information on available components and flows.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/regenerate.json
     */
    public boolean regenerate() throws TransmissionException {
        String reqPath = "/services/repository/regenerate.json";
        JSONTokener jt = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jt);
            String sSuccess = "Repository successfully regenerated";
            
            return (sSuccess.equals(ja.getJSONObject(0).getString("message")));
        }
        catch (JSONException e) {
            return false;
        }
    }

    /**
     * requests the urls of all components in the server repository.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/list_components.json
     * @throws TransmissionException
     */
    public Set<URI> retrieveComponentUris() throws TransmissionException {
        String reqPath = "/services/repository/list_components.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setCompURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setCompURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setCompURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests the urls of all flows in the server repository.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/list_flows.json
     * @throws TransmissionException
     */
    public Set<URI> retrieveFlowUris() throws TransmissionException {
        String reqPath = "/services/repository/list_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setCompURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setCompURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setCompURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }


    /**
     * requests all tags for any and all components and flows.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags.json
     * @throws TransmissionException
     *
     * TODO:return tag objects instead of strings
     */
    public Set<String> retrieveAllTags() throws TransmissionException {
        String reqPath = "/services/repository/tags.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<String> setTags = new HashSet<String>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setTags.add(ja.getJSONObject(i).getString("meandre_tag"));
            
            return setTags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests all tags for all components.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags_components.json
     * @throws TransmissionException
     * TODO:return tag objects instead of strings
     */
    public Set<String> retrieveComponentTags() throws TransmissionException {
        String reqPath = "/services/repository/tags_components.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<String> setTags = new HashSet<String>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setTags.add(ja.getJSONObject(i).getString("meandre_tag"));
            
            return setTags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests all tags for all flows.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/tags_flows.json
     * @throws TransmissionException
     * TODO:return tag objects instead of strings
     */
    public Set<String> retrieveFlowTags() throws TransmissionException {
        String reqPath = "/services/repository/tags_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<String> setTags = new HashSet<String>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setTags.add(ja.getJSONObject(i).getString("meandre_tag"));
            
            return setTags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests the urls of all components that have the input tag.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/components_by_tag.json
     * @throws TransmissionException
     * TODO:input a tag object instead of string
     */
    public Set<URI> retrieveComponentsByTag(String tag) throws TransmissionException {
        String argPath = "/services/repository/components_by_tag.json";
        NameValuePair argTag = new BasicNameValuePair("tag", tag);
        JSONTokener jtRetrieved = _httpClient.doGET(argPath, null, JSONResponseHandler.getInstance(), argTag);

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests the urls of all flows that have the input tag.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/flows_by_tag.json
     * @throws TransmissionException
     * TODO:input a tag object instead of string
     */
    public Set<URI> retrieveFlowsByTag(String tag) throws TransmissionException {
        String reqPath = "/services/repository/flows_by_tag.json";
        NameValuePair argTag = new BasicNameValuePair("tag", tag);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argTag);
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * requests the component description model from the server.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/describe_component.nt
     * @throws TransmissionException
     */
    public ExecutableComponentDescription retrieveComponentDescriptor(String componentUri) throws TransmissionException {
        String reqPath = "/services/repository/describe_component.nt";
        NameValuePair argCompUri = new BasicNameValuePair("uri", componentUri);
        Model compModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance(), argCompUri);

        QueryableRepository qr = new RepositoryImpl(compModel);
        Set<ExecutableComponentDescription> repoComps = qr.getAvailableExecutableComponentDescriptions();
        Iterator<ExecutableComponentDescription> iter = repoComps.iterator();
        ExecutableComponentDescription comp = iter.next();

        if (iter.hasNext())
            throw new TransmissionException("More than one component descriptor was returned by the server.");

        return comp;
    }

    /**
     * requests a flow description model from the server.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/describe_flow.nt
     * @throws TransmissionException
     */
    public FlowDescription retrieveFlowDescriptor(String flowUri) throws TransmissionException {
        String reqPath = "/services/repository/describe_flow.nt";
        NameValuePair argFlowUri = new BasicNameValuePair("uri", flowUri);
        Model flowModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance(), argFlowUri);

        QueryableRepository qr = new RepositoryImpl(flowModel);
        Set<FlowDescription> repoFlows = qr.getAvailableFlowDescriptions();
        Iterator<FlowDescription> iter = repoFlows.iterator();
        FlowDescription flow = iter.next();

        if (iter.hasNext())
            throw new TransmissionException("More than one flow descriptor was returned by the server.");
        
        return flow;
    }

    /**
     *TODO: need serious docs on this or a query object to input instead of
     * a string.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/search_components.json
     * @throws TransmissionException
     */
    public Set<URI> retrieveComponentUrlsByQuery(String query) throws TransmissionException {
        String reqPath = "/services/repository/search_components.json";
        NameValuePair argQuery = new BasicNameValuePair("q", query);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argQuery);

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setCompURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setCompURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setCompURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * TODO: need serious docs on this or a query object to input instead of
     * a string.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/search_flows.json
     * @throws TransmissionException
     */
    public Set<URI> retrieveFlowUrlsByQuery(String query) throws TransmissionException {
        String reqPath = "/services/repository/search_flows.json";
        NameValuePair argQuery = new BasicNameValuePair("q", query);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argQuery);

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Set<URI> setCompURIs = new HashSet<URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++)
                setCompURIs.add(new URI(ja.getJSONObject(i).getString("meandre_uri")));
            
            return setCompURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * uploads a single flow to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     *
     */
    public boolean uploadFlow(FlowDescription flow, boolean overwrite) throws TransmissionException {
        return uploadModel(flow.getModel(), null, overwrite);
    }

    /**
     * uploads a set of flows to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public boolean uploadFlowBatch(Set<FlowDescription> flows, boolean overwrite) throws TransmissionException {
        HashSet<Model> hsFlowModels = new HashSet<Model>();
        Iterator<FlowDescription> flowIter = flows.iterator();
        while (flowIter.hasNext())
            hsFlowModels.add(flowIter.next().getModel());
        
        return uploadModelBatch(hsFlowModels, null, overwrite);
    }

    /**
     * uploads a single component to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     *
     */
    public boolean uploadComponent(ExecutableComponentDescription component, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        return uploadModel(component.getModel(), jarFileContexts, overwrite);
    }

    /**
     * uploads a set of flows to the server.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    public boolean uploadComponentBatch(Set<ExecutableComponentDescription> components, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        HashSet<Model> hsComponentModels = new HashSet<Model>();
        Iterator<ExecutableComponentDescription> compIter = components.iterator();
        while (compIter.hasNext())
            hsComponentModels.add(compIter.next().getModel());
        
        return uploadModelBatch(hsComponentModels, jarFileContexts, overwrite);
    }

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
    public boolean uploadRepository(QueryableRepository qr, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        return uploadModel(qr.getModel(), jarFileContexts, overwrite);
    }

    /**
     * uploads a single model containing flows and/or components and any
     * jar files.
     * the jarfiles set may be null.
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/repository/add.nt
     * TODO:Need test
     */
    private boolean uploadModel(Model model, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        HashSet<Model> modSet = new HashSet<Model>(1);
        modSet.add(model);
        
        return uploadModelBatch(modSet, jarFileContexts, overwrite);
    }

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
    public boolean uploadModelBatch(Set<Model> resModels, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        String reqPath = "/services/repository/add.json";

        NameValuePair[] nvps = new BasicNameValuePair[2];
        nvps[0] = new BasicNameValuePair("overwrite", Boolean.toString(overwrite));
        nvps[1] = new BasicNameValuePair("dump", "false");

        List<KeyValuePair<String, ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();

        try {
            for (Model modUpload : resModels) {
                String sModel = ModelUtils.modelToDialect(modUpload, "N-TRIPLE");
                parts.add(new KeyValuePair<String, ContentBody>("repository", new StringBody(sModel)));
            }
    
            if (jarFileContexts != null)
                for (File jarFile : jarFileContexts) 
                    if (jarFile.exists())
                        parts.add(new KeyValuePair<String, ContentBody>("context", new FileBody(jarFile)));
                    else
                        throw new TransmissionException(new FileNotFoundException(jarFile.toString()));
            
            _httpClient.doPOST(reqPath, null, parts, StringResponseHandler.getInstance(), nvps);
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        
        return true;
    }

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
    public boolean uploadFiles(Set<File> files, boolean overwrite) throws TransmissionException {
        //just use the regular uploader with no models
        Set<Model> emptyModelSet = new HashSet<Model>(0);
        
        return uploadModelBatch(emptyModelSet, files, overwrite);
    }

    /**
     *removes (deletes) either a component or flow from the server. returns
     *true if the resource was deleted.
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/repository/remove.json
     * TODO: need more specific error reporting when the server returns an empty
     * json string
     */
    public boolean removeResource(String resourceUri) throws TransmissionException{
        String reqPath = "/services/repository/remove.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);
        
        try{
            JSONArray ja = new JSONArray(jtRetrieved);
            
            JSONObject joRetrieved = ja.getJSONObject(0);
            String sRetrieved = joRetrieved.getString("meandre_uri");

            return sRetrieved.equals(resourceUri);
        } 
        catch (JSONException e) {
            return false;
        }
    }

    /////////
    //Publish
    /////////

    /**
     * commands the server to change a component or flow's status to "published."
     * returns true if the resource is in a state of "published" after this
     * method returns.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/services/publish/publish.json
     */
    public boolean publish(String resourceUri) throws TransmissionException {
        String reqPath = "/services/publish/publish.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);

        try {
           JSONArray ja = new JSONArray(jtRetrieved);
           
           return resourceUri.equals(ja.getJSONObject(0).getString("meandre_uri"));
        }
        catch (JSONException e) {
            return false;
        }
    }

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
    public boolean unpublish(String resourceUri) throws TransmissionException {
        String reqPath = "/services/publish/unpublish.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            return resourceUri.equals(ja.getJSONObject(0).getString("meandre_uri"));
        }
        catch (JSONException e) {
            return false;
        }
    }

    /////////
    //Execution
    ///////////

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
    public String runFlow(String flowUri, boolean verbose) throws TransmissionException {
        String reqPath = "/services/execute/flow.txt";
        
        NameValuePair[] nvps = new BasicNameValuePair[2];
        nvps[0] = new BasicNameValuePair("uri", flowUri);
        nvps[1] = new BasicNameValuePair("statistics", Boolean.toString(verbose));

        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance(), nvps);
    }

    /**
     * Executes the flow with the list of probes that will be turned on.
     * 
     * @param flowUri
     * @param probeList
     * @return
     * @throws TransmissionException
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/execute/flow.txt
     */
    public String runFlow(String flowUri, HashMap<String,String> probeList) throws TransmissionException {
        String reqPath = "/services/execute/flow.txt";
        
        Set<NameValuePair> nvps = new HashSet<NameValuePair>();
        nvps.add(new BasicNameValuePair("uri", flowUri));
        
        for (Entry<String, String> probe : probeList.entrySet())
            nvps.add(new BasicNameValuePair(probe.getKey(), probe.getValue()));
        
        NameValuePair[] args = new BasicNameValuePair[nvps.size()];
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance(), nvps.toArray(args));
    }

    /**
     * This method uploads and executes all the flows in the provided model
     *
     * <p> calls:
     * http://<meandre_host>:<meandre_port>/services/execute/repository.txt
     * TODO:Need test
     */
    public String runRepository(Model model) throws TransmissionException {
        String reqPath = "/services/execute/repository.txt";
        List<KeyValuePair<String, ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();
        String sModel = ModelUtils.modelToDialect(model, "N-TRIPLE");

        try {
            parts.add(new KeyValuePair<String, ContentBody>("repository", new StringBody(sModel)));
            return _httpClient.doPOST(reqPath, null, parts, StringResponseHandler.getInstance());
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
    }

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
    public InputStream runFlowStreamOutput(String flowUri, boolean verbose) throws TransmissionException {
        return runFlowStreamOutput(flowUri, null, verbose);
    }

    public InputStream runFlowStreamOutput(String flowUri, String token, boolean verbose) throws TransmissionException {
        String reqPath = "/services/execute/flow.txt";
        
        Set<NameValuePair> nvps = new HashSet<NameValuePair>();
        nvps.add(new BasicNameValuePair("uri", flowUri));
        nvps.add(new BasicNameValuePair("statistics", Boolean.toString(verbose)));
        
        if (token != null)
            nvps.add(new BasicNameValuePair("token", token));

        NameValuePair[] args = new BasicNameValuePair[nvps.size()];
        return _httpClient.doGET(reqPath, null, StreamResponseHandler.getInstance(), nvps.toArray(args));
    }

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
    public JSONObject retrieveWebUIInfo(String token) throws TransmissionException {
        String reqPath = "/services/execute/uri_flow.txt";
        NameValuePair argToken = new BasicNameValuePair("token", token);
        InputStream results = _httpClient.doGET(reqPath, null, StreamResponseHandler.getInstance(), argToken);

        Properties properties = new Properties();
        try {
            properties.load(results);
        }
        catch (IOException e) {
            throw new TransmissionException(e);
        }

        JSONObject joWebUIInfo = (properties.isEmpty()) ? null : new JSONObject();

        for (Entry<Object, Object> prop : properties.entrySet())
            try {
                joWebUIInfo.put(prop.getKey().toString(), prop.getValue());
            }
            catch (JSONException e) {
                throw new TransmissionException(e);
            }

        return joWebUIInfo;
    }

    /**
     * returns the url name of any running flows and the url assigned to
     * the webui component of the flow.
     *
     * @return a map where the keys are flow id urls, and the values are webui
     * urls
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/execute/list_running_flows.json
     *TODO: need to reverse the order in the map so that the always unique
     * webui_url is the key and the not-always-unique flow intance url is
     * the value. requires a server side change.
     * FIXME: This is totally untested.
     */
    public Map<URI,URI> retrieveRunningFlows() throws TransmissionException {
        String reqPath = "/services/execute/list_running_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Map<URI, URI> muRetrievedPairs = new Hashtable<URI,URI>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++) {
                JSONObject jo = ja.getJSONObject(i);
                muRetrievedPairs.put(
                        new URI(jo.getString("flow_instance_uri")),
                        new URI(jo.getString("flow_instance_webui_uri"))
                );
            }
            
            return muRetrievedPairs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * returns the url name of any running flows and the urls assigned to
     * the webui component of the flow.
     *
     * @return a map where the keys are flow id urls, and the values are webui
     * urls
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/services/execute/list_running_flows.json
     *TODO: need to reverse the order in the map so that the always unique
     * webui_url is the key and the not-always-unique flow intance url is
     * the value. requires a server side change.
     * FIXME: This is totally untested.
     */
    public Map<URI,Map<String,URI>> retrieveRunningFlowsInformation() throws TransmissionException{
        String reqPath = "/services/execute/list_running_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Map<URI, Map<String,URI>> muRetrievedPairs = new Hashtable<URI,Map<String,URI>>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++) {
                JSONObject jo = ja.getJSONObject(i);
                Map<String,URI> map = new Hashtable<String,URI>(3);
                URI furi = new URI(jo.getString("flow_instance_uri"));
                map.put("flow_instance_uri",furi);
                map.put("flow_instance_webui_uri",new URI(jo.getString("flow_instance_webui_uri")));
                map.put("flow_instance_proxy_webui_uri",new URI(jo.getString("flow_instance_proxy_webui_uri")));
                muRetrievedPairs.put(furi,map);
            }
            
            return muRetrievedPairs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

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
    public Vector<Map<String,String>> retrieveJobStatuses() throws TransmissionException {
        String reqPath = "/services/jobs/list_jobs_statuses.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray ja = new JSONArray(jtRetrieved);
            
            Vector<Map<String,String>> vecStatuses = new Vector<Map<String,String>>();
            for (int i = 0, iMax = ja.length(); i < iMax; i++) {
                JSONObject jo = ja.getJSONObject(i);
                Map<String,String> map = new Hashtable<String,String>(4);
                map.put("status",jo.getString("status"));
                map.put("server_id",jo.getString("server_id"));
                map.put("ts",jo.getString("ts"));
                map.put("job_id",jo.getString("job_id"));
                vecStatuses.add(map);
            }
            
            return vecStatuses;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

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
    public String retrieveJobConsole(String sFUID) throws TransmissionException{
        String reqPath = "/services/jobs/job_console.json";
        NameValuePair argUri = new BasicNameValuePair("uri", sFUID);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argUri);

        try {    
            JSONArray ja = new JSONArray(jtRetrieved);
            
            return ja.getJSONObject(0).getString("console");
        }
        catch (JSONException e) {
            return "Console not available";
        }
    }

    ////////////
    //Public
    //////////////

    /**
     * retrieves the public repository of published resources. does not
     * require authorization.
     *
     *<p> calls:
     * http://<meandre_host>:<meandre_port>/public/services/repository.nt
     * @throws TransmissionException
     */
    public QueryableRepository retrievePublicRepository() throws TransmissionException {
        String reqPath = "/public/services/repository.nt";
        InputStream modelStream = _httpClient.doGET(reqPath, null, StreamResponseHandler.getInstance());

        Model model = ModelFactory.createDefaultModel();
        model.read(modelStream, null, "N-TRIPLE");
        
        return new RepositoryImpl(model);
    }

    /**
     * retrieves the demo repository of published resources. does not
     * require authorization
     *
     *<p> calls:
     *http://<meandre_host>:<meandre_port>/public/services/demo_repository.nt
     */
    public QueryableRepository retrieveDemoRepository() throws TransmissionException {
        String reqPath = "/public/services/demo_repository.nt";
        InputStream modelStream = _httpClient.doGET(reqPath, null, StreamResponseHandler.getInstance());
        
        Model model = ModelFactory.createDefaultModel();
        model.read(modelStream, null, "N-TRIPLE");
        
        return new RepositoryImpl(model);
    }

    ////////////////////////
    //Admin of Running Flows
    ////////////////////////

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
    public boolean abortFlow(int webUIPort) throws TransmissionException {
        String reqPath = "/admin/abort.txt";
        String sExpected = "Abort request dispatched..."; 
        
        GenericHttpClient client = new GenericHttpClient(_httpClient.getHost().getHostName(), webUIPort);
        try {
            String sRetrieved = client.doGET(reqPath, null, StringResponseHandler.getInstance());
            
            return sRetrieved.equals(sExpected);
        }
        finally {
            client.close();
        }
    }

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
    public JSONObject retrieveRunningFlowStatisitics(int webUIPort) throws TransmissionException {
        String reqPath = "/admin/statistics.json";

        GenericHttpClient client = new GenericHttpClient(_httpClient.getHost().getHostName(), webUIPort);
        try {
            JSONTokener jtRetrieved = client.doGET(reqPath, null, JSONResponseHandler.getInstance());
            
            return new JSONObject(jtRetrieved);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        finally {
            client.close();
        }
    }

    //----- Amit's patch comented by Xavier and are UNTESTED ----
    // TODO: Write test cases for the methods below

    /** 
     * Return Component Descriptor as String
     *
     * @param componentUri The component url
     * @return The RDF description of the component
     * @throws TransmissionException The component could not be retrieved
     */
    public String retrieveComponentDescriptorAsString(String componentUri) throws TransmissionException {
        String reqPath = "/services/repository/describe_component.nt";
        NameValuePair argCompUri = new BasicNameValuePair("uri", componentUri);
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance(), argCompUri);
    }

    /** 
     * Gets the server version.
     *
     * @return The server version
     * @throws TransmissionException Could not get the server version
     */
    public String getServerVersion() throws TransmissionException {
        String reqPath = "/services/about/version.txt";
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance());
    }

    /** 
     * Return the JSON content describing the plugins available.
     *
     * @return The JSON string
     * @throws TransmissionException Fail to retrieve the plugins' information
     */
    public String getServerPlugins() throws TransmissionException {
        String reqPath = "/services/about/plugins.json";
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance());
    }

    /** 
     * Returns jar information
     *
     * @param jarFile The jar file to get the info from
     * @return Returns the information associated to the jar
     * @throws TransmissionException The plugin failed to retrieve the information
     */
    public String getComponentJarInfo(String jarFile) throws TransmissionException {
        String reqPath = "/plugin/jar/" + jarFile + "/info";
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance());
    }

    /** 
     * Pings the server
     *
     *	@return True if it successfully pinged the server
     */
    public boolean ping() throws TransmissionException {
        String reqPath = "/public/services/ping.txt";
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance()) != null;
    }
}
