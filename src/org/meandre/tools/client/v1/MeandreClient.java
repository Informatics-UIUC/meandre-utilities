package org.meandre.tools.client.v1;

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
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.LocationBean;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.tools.client.AbstractMeandreClient;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.meandre.tools.client.utils.ClientLoggerFactory;
import org.meandre.tools.client.utils.GenericHttpClient;
import org.meandre.tools.client.utils.handlers.JSONResponseHandler;
import org.meandre.tools.client.utils.handlers.RDFModelResponseHandler;
import org.meandre.tools.client.utils.handlers.StringResponseHandler;
import org.seasr.meandre.support.generic.io.ModelUtils;
import org.seasr.meandre.support.generic.util.KeyValuePair;

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
 * Dependencies required for this to work:
 * 
 * commons-codec-1.3.jar
 * commons-logging-1.1.1.jar
 * httpclient-4.0.3.jar
 * httpcore-4.0.1.jar
 * httpmime-4.0.3.jar
 * apache-mime4j-0.6.jar
 * 
 * arq-2.8.4.jar
 * icu4j-3.4.4.jar
 * iri-0.8.jar
 * jena-2.6.3.jar
 * slf4j-api-1.5.8.jar
 * slf4j-log4j12-1.5.8.jar
 * log4j-1.2.13.jar
 * lucene-core-2.3.1.jar
 * stax-api-1.0.1.jar
 * wstx-asl-3.2.9.jar
 * xercesImpl-2.7.1.jar
 *
 * json.jar
 *
 * meandre-kernel-1.4.9.jar
 * seasr-commons.jar
 *
 * @author Boris Capitanu
 */
public class MeandreClient extends AbstractMeandreClient {

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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#setCredentials(java.lang.String, java.lang.String)
     */
    @Override
    public void setCredentials(String userName, String password) {
        _httpClient.setCredentials(userName, password);
    }
    
    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#setLogger(java.util.logging.Logger)
     */
    @Override
    public void setLogger(Logger logger) {
        _httpClient.setLogger(logger);
    }
    
    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#getLogger()
     */
    @Override
    public Logger getLogger() {
        return _httpClient.getLogger();
    }
    
    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#close()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveUserRoles()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveValidRoles()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveLocations()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#addLocation(java.lang.String, java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#removeLocation(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveRepository()
     */
    @Override
    public QueryableRepository retrieveRepository() throws TransmissionException {
        String reqPath = "/services/repository/dump.nt";
        Model model = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(model);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#regenerate()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveComponentUris()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveFlowUris()
     */
    @Override
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


    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveAllTags()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveComponentTags()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveFlowTags()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveComponentsByTag(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveFlowsByTag(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveComponentDescriptor(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveFlowDescriptor(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveComponentUrlsByQuery(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveFlowUrlsByQuery(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadFlow(org.meandre.core.repository.FlowDescription, boolean)
     */
    @Override
    public boolean uploadFlow(FlowDescription flow, boolean overwrite) throws TransmissionException {
        return uploadModel(flow.getModel(), null, overwrite);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadFlowBatch(java.util.Set, boolean)
     */
    @Override
    public boolean uploadFlowBatch(Set<FlowDescription> flows, boolean overwrite) throws TransmissionException {
        HashSet<Model> hsFlowModels = new HashSet<Model>();
        Iterator<FlowDescription> flowIter = flows.iterator();
        while (flowIter.hasNext())
            hsFlowModels.add(flowIter.next().getModel());
        
        return uploadModelBatch(hsFlowModels, null, overwrite);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadComponent(org.meandre.core.repository.ExecutableComponentDescription, java.util.Set, boolean)
     */
    @Override
    public boolean uploadComponent(ExecutableComponentDescription component, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        return uploadModel(component.getModel(), jarFileContexts, overwrite);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadComponentBatch(java.util.Set, java.util.Set, boolean)
     */
    @Override
    public boolean uploadComponentBatch(Set<ExecutableComponentDescription> components, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        HashSet<Model> hsComponentModels = new HashSet<Model>();
        Iterator<ExecutableComponentDescription> compIter = components.iterator();
        while (compIter.hasNext())
            hsComponentModels.add(compIter.next().getModel());
        
        return uploadModelBatch(hsComponentModels, jarFileContexts, overwrite);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadRepository(org.meandre.core.repository.QueryableRepository, java.util.Set, boolean)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadModelBatch(java.util.Set, java.util.Set, boolean)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#uploadFiles(java.util.Set, boolean)
     */
    @Override
    public boolean uploadFiles(Set<File> files, boolean overwrite) throws TransmissionException {
        //just use the regular uploader with no models
        Set<Model> emptyModelSet = new HashSet<Model>(0);
        
        return uploadModelBatch(emptyModelSet, files, overwrite);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#removeResource(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#publish(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#unpublish(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#runFlow(java.lang.String, boolean)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#runRepository(com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#runFlowStreamOutput(java.lang.String, boolean)
     */
    @Override
    public InputStream runFlowStreamOutput(String flowUri, boolean verbose) throws TransmissionException {
        return runFlowStreamOutput(flowUri, null, verbose);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#runFlowStreamOutput(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public InputStream runFlowStreamOutput(String flowUri, String token, boolean verbose) throws TransmissionException {
        String reqPath = "/services/execute/flow.txt";
        
        Set<NameValuePair> nvps = new HashSet<NameValuePair>();
        nvps.add(new BasicNameValuePair("uri", flowUri));
        nvps.add(new BasicNameValuePair("statistics", Boolean.toString(verbose)));
        
        if (token != null)
            nvps.add(new BasicNameValuePair("token", token));

        NameValuePair[] args = new BasicNameValuePair[nvps.size()];
        return _httpClient.doGET(reqPath, null, nvps.toArray(args));
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveWebUIInfo(java.lang.String)
     */
    @Override
    public JSONObject retrieveWebUIInfo(String token) throws TransmissionException {
        String reqPath = "/services/execute/uri_flow.txt";
        NameValuePair argToken = new BasicNameValuePair("token", token);
        InputStream results = _httpClient.doGET(reqPath, null, argToken);

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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveJobStatuses()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveJobConsole(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrievePublicRepository()
     */
    @Override
    public QueryableRepository retrievePublicRepository() throws TransmissionException {
        String reqPath = "/public/services/repository.nt";
        InputStream modelStream = _httpClient.doGET(reqPath, null);

        Model model = ModelFactory.createDefaultModel();
        model.read(modelStream, null, "N-TRIPLE");
        
        return new RepositoryImpl(model);
    }

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveDemoRepository()
     */
    @Override
    public QueryableRepository retrieveDemoRepository() throws TransmissionException {
        String reqPath = "/public/services/demo_repository.nt";
        InputStream modelStream = _httpClient.doGET(reqPath, null);
        
        Model model = ModelFactory.createDefaultModel();
        model.read(modelStream, null, "N-TRIPLE");
        
        return new RepositoryImpl(model);
    }

    ////////////////////////
    //Admin of Running Flows
    ////////////////////////

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#abortFlow(int)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#retrieveRunningFlowStatisitics(int)
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#getServerVersion()
     */
    @Override
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

    /* (non-Javadoc)
     * @see org.meandre.client.v1.IMeandreClient#ping()
     */
    @Override
    public boolean ping() throws TransmissionException {
        String reqPath = "/public/services/ping.txt";
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance()) != null;
    }

    @Override
    public String getHostName() {
        return _httpClient.getHost().getHostName();
    }

    @Override
    public int getPort() {
        return _httpClient.getHost().getPort();
    }
}
