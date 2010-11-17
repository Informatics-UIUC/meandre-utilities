package org.meandre.tools.client.v2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.meandre.tools.client.exceptions.OperationFailedException;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.meandre.tools.client.utils.GenericLoggerFactory;
import org.meandre.tools.client.utils.GenericHttpClient;
import org.meandre.tools.client.utils.handlers.JSONResponseHandler;
import org.meandre.tools.client.utils.handlers.RDFModelResponseHandler;
import org.meandre.tools.client.utils.handlers.StringResponseHandler;
import org.seasr.meandre.support.generic.io.ModelUtils;
import org.seasr.meandre.support.generic.util.KeyValuePair;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


/**
 * Programmatic interface to the Meandre server webservices API.
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
 * log4j-1.2.13.jar
 * slf4j-api-1.5.8.jar
 * slf4j-log4j12-1.5.8.jar
 * stax-api-1.0.1.jar
 * wstx-asl-3.2.9.jar
 * xercesImpl-2.7.1.jar
 * lucene-core-2.3.1.jar
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
     * @param serverHost just the hostname, e.g. "localhost", NOT "http://localhost"
     * @param port the port on the serverHost that the server is listening on
     */
    public MeandreClient(String serverHost, int port) {
        _httpClient = new GenericHttpClient(serverHost, port, GenericLoggerFactory.getLogger());
    }

    @Override
    public void setCredentials(String userName, String password) {
        _httpClient.setCredentials(userName, password);
    }
    
    @Override
    public void setLogger(Logger logger) {
        _httpClient.setLogger(logger);
    }
    
    @Override
    public Logger getLogger() {
        return _httpClient.getLogger();
    }
    
    @Override
    public void close() {
        _httpClient.close();
    }

    /**
     * /services/security/user.json
     * 
     * @return The set of user roles
     * @throws TransmissionException
     */
    @Override
    public Set<String> retrieveUserRoles() throws TransmissionException {
        String reqPath = "/services/security/user.json";
        JSONTokener jtRoles = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONObject joUser = getSuccessPayload(jtRoles).getJSONObject("user");
            JSONArray jaRoles = joUser.getJSONArray("roles");
            
            Set<String> roles = new HashSet<String>();
            for (int i = 0, iMax = jaRoles.length(); i < iMax; i++)
                roles.add(jaRoles.getString(i));

            return roles;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }
    
    /**
     * /services/security/valid_roles.json
     * 
     * @return The set of valid roles
     * @throws TransmissionException
     */
    @Override
    public Set<String> retrieveValidRoles() throws TransmissionException {
        String reqPath = "/services/security/valid_roles.json";
        JSONTokener jtRoles = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONArray jaRoles = getSuccessPayload(jtRoles).getJSONArray("roles");
            
            Set<String> roles = new HashSet<String>();
            for (int i = 0, iMax = jaRoles.length(); i < iMax; i++)
                roles.add(jaRoles.getString(i));

            return roles;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }

    /////////
    //Locations (known peers of the server)
    //////////

    /**
     * /services/locations/list.json
     * 
     * @return The list of locations
     * @throws TransmissionException
     */
    @Override
    public Set<LocationBean> retrieveLocations() throws TransmissionException {
        String reqPath = "/services/locations/list.json";
        JSONTokener jtLocs = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray jaLocations = getSuccessPayload(jtLocs).getJSONArray("locations");
            
            HashSet<LocationBean> locations = new HashSet<LocationBean>();
            for (int i = 0, iMax = jaLocations.length(); i < iMax; i++) {
                JSONObject joLocation = jaLocations.getJSONObject(i);
                locations.add(new LocationBean(joLocation.getString("location"), joLocation.getString("description")));
            }

            return locations;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/locations/add.json
     *
     * @param locationUrl The location URL to add
     * @param description The description for the location
     * @return True for success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean addLocation(String locationUrl, String description) throws TransmissionException {
        String reqPath = "/services/locations/add.json";

        NameValuePair[] nvps = new BasicNameValuePair[2];
        nvps[0] = new BasicNameValuePair("location", locationUrl);
        nvps[1] = new BasicNameValuePair("description", description);

        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), nvps);
        
        try {
            return getSuccessPayload(jtRetrieved).has("added_uris");  
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /**
     * /services/locations/remove.json
     * 
     * @param locationUrl The location URL to remove
     * @return True for success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean removeLocation(String locationUrl) throws TransmissionException {
        String reqPath = "/services/locations/remove.json";

        NameValuePair argLoc = new BasicNameValuePair("location", locationUrl);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argLoc);
        
        try {
            JSONArray jaRemoved = getSuccessPayload(jtRetrieved).getJSONArray("removed_locations");
            String location = jaRemoved.getString(0);
            
            return location.equals(locationUrl);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    ////////////
    //Repository
    /////////////

    /**
     * /services/repository/describe.nt
     * 
     * @return A QueryableRepository for programmatic access to the repository
     * @throws TransmissionException
     */
    @Override
    public QueryableRepository retrieveRepository() throws TransmissionException {
        String reqPath = "/services/repository/describe.nt";
        Model model = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(model);
    }

    /**
     * /services/repository/regenerate.json
     * 
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean regenerate() throws TransmissionException {
        String reqPath = "/services/repository/regenerate.json";
        JSONTokener jt = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            return getSuccessPayload(jt).has("added_uris");
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /**
     * /services/repository/list_components.json
     * 
     * @return The set of component URIs
     * @throws TransmissionException
     */
    @Override
    public Set<URI> retrieveComponentUris() throws TransmissionException {
        String reqPath = "/services/repository/list_components.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray jaComponents = getSuccessPayload(jtRetrieved).getJSONArray("components");
            
            Set<URI> compURIs = new HashSet<URI>();
            for (int i = 0, iMax = jaComponents.length(); i < iMax; i++)
                compURIs.add(new URI(jaComponents.getJSONObject(i).getString("uri")));
            
            return compURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/repository/list_flows.json
     * 
     * @return The set of flow URIs
     * @throws TransmissionException
     */
    @Override
    public Set<URI> retrieveFlowUris() throws TransmissionException {
        String reqPath = "/services/repository/list_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONArray jaComponents = getSuccessPayload(jtRetrieved).getJSONArray("flows");
            
            Set<URI> flowURIs = new HashSet<URI>();
            for (int i = 0, iMax = jaComponents.length(); i < iMax; i++)
                flowURIs.add(new URI(jaComponents.getJSONObject(i).getString("uri")));
            
            return flowURIs;
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/repository/tags.json
     * 
     * @return The set of all tags
     * @throws TransmissionException
     */
    @Override
    public Set<String> retrieveAllTags() throws TransmissionException {
        String reqPath = "/services/repository/tags.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONObject joTags = getSuccessPayload(jtRetrieved).getJSONObject("tags");
            
            Set<String> tags = new HashSet<String>();
            @SuppressWarnings("unchecked")
            Iterator<String> iter = joTags.keys();
            while (iter.hasNext())
                tags.add(iter.next());
            
            return tags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/repository/tags_components.json
     * 
     * @return The set of all component tags
     * @throws TransmissionException
     */
    @Override
    public Set<String> retrieveComponentTags() throws TransmissionException {
        String reqPath = "/services/repository/tags_components.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            JSONObject joTags = getSuccessPayload(jtRetrieved).getJSONObject("tags");
            
            Set<String> tags = new HashSet<String>();
            @SuppressWarnings("unchecked")
            Iterator<String> iter = joTags.keys();
            while (iter.hasNext())
                tags.add(iter.next());
            
            return tags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/repository/tags_flows.json
     * 
     * @return The set of all flow tags
     * @throws TransmissionException
     */
    @Override
    public Set<String> retrieveFlowTags() throws TransmissionException {
        String reqPath = "/services/repository/tags_flows.json";
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            JSONObject joTags = getSuccessPayload(jtRetrieved).getJSONObject("tags");
            
            Set<String> tags = new HashSet<String>();
            @SuppressWarnings("unchecked")
            Iterator<String> iter = joTags.keys();
            while (iter.hasNext())
                tags.add(iter.next());
            
            return tags;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public Set<URI> retrieveComponentsByTag(String tag) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public Set<URI> retrieveFlowsByTag(String tag) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * /services/repository/describe.nt
     * 
     * @param componentUri The component URI to retrieve
     * @throws TransmissionException
     */
    @Override
    public ExecutableComponentDescription retrieveComponentDescriptor(String componentUri) throws TransmissionException {
        String reqPath = "/services/repository/describe.nt";
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
     * /services/repository/describe.nt
     * 
     * @param flowUri The flow URI to retrieve
     * @throws TransmissionException
     */
    @Override
    public FlowDescription retrieveFlowDescriptor(String flowUri) throws TransmissionException {
        String reqPath = "/services/repository/describe.nt";
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
     * @throws NotImplementedException
     */
    @Override
    public Set<URI> retrieveComponentUrlsByQuery(String query) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public Set<URI> retrieveFlowUrlsByQuery(String query) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * /services/repository/add.json
     *
     * @param flow The flow
     * @param overwrite True to overwrite existing flow, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean uploadFlow(FlowDescription flow, boolean overwrite) throws TransmissionException {
        return uploadModel(flow.getModel(), null, overwrite);
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public boolean uploadFlowBatch(Set<FlowDescription> flows, boolean overwrite) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * /services/repository/add.json
     *
     * @param component The component
     * @param contexts The component contexts
     * @param overwrite True to overwrite existing component, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean uploadComponent(ExecutableComponentDescription component, Set<File> contexts, boolean overwrite) throws TransmissionException {
        return uploadModel(component.getModel(), contexts, overwrite);
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public boolean uploadComponentBatch(Set<ExecutableComponentDescription> components, Set<File> jarFileContexts, boolean overwrite) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * /services/repository/add.json
     * 
     * @param qr The QueryableRepository to upload
     * @param contexts The contexts
     * @param overwrite True to overwrite, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean uploadRepository(QueryableRepository qr, Set<File> contexts, boolean overwrite) throws TransmissionException {
        return uploadModel(qr.getModel(), contexts, overwrite);
    }

    /**
     * /services/repository/add.json
     * 
     * @param model The model
     * @param contexts The contexts
     * @param overwrite True to overwrite, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    private boolean uploadModel(Model model, Set<File> contexts, boolean overwrite) throws TransmissionException {
        HashSet<Model> modSet = new HashSet<Model>(1);
        modSet.add(model);
        
        return uploadModelBatch(modSet, contexts, overwrite);
    }

    /**
     * /services/repository/add.json
     * 
     * @param models The models to upload
     * @param contexts The contexts
     * @param overwrite True to overwrite, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean uploadModelBatch(Set<Model> models, Set<File> contexts, boolean overwrite) throws TransmissionException {
        String reqPath = "/services/repository/add.json";
        
        NameValuePair argOverwrite = new BasicNameValuePair("overwrite", Boolean.toString(overwrite));
        List<KeyValuePair<String, ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();

        try {
            for (Model modUpload : models) {
                String sModel = ModelUtils.modelToDialect(modUpload, "N-TRIPLE");
                parts.add(new KeyValuePair<String, ContentBody>("repository", new StringBody(sModel)));
            }
    
            if (contexts != null)
                for (File jarFile : contexts) 
                    if (jarFile.exists())
                        parts.add(new KeyValuePair<String, ContentBody>("context", new FileBody(jarFile)));
                    else
                        throw new TransmissionException(new FileNotFoundException(jarFile.toString()));
            
            JSONTokener response = _httpClient.doPOST(reqPath, null, parts, JSONResponseHandler.getInstance(), argOverwrite);
            return getSuccessPayload(response).has("uris");
        }
        catch (OperationFailedException e) {
            return false;
        }       
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }

    /**
     * /services/repository/add.json
     * 
     * @param files The files
     * @param overwrite True to overwrite, False otherwise
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean uploadFiles(Set<File> files, boolean overwrite) throws TransmissionException {
        //just use the regular uploader with no models
        Set<Model> emptyModelSet = new HashSet<Model>(0);
        
        return uploadModelBatch(emptyModelSet, files, overwrite);
    }

    /**
     * /services/repository/remove.json
     * 
     * @param resourceUri The resource URI
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean removeResource(String resourceUri) throws TransmissionException {
        String reqPath = "/services/repository/remove.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);
        
        try{
            JSONArray jaRemoved = getSuccessPayload(jtRetrieved).getJSONArray("uris");
            
            return jaRemoved.length() == 1 && jaRemoved.getString(0).equals(resourceUri);
        } 
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /////////
    //Publish
    /////////

    /**
     * /services/publish/publish.json
     * 
     * @param resourceUri The resource URI to publish
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean publish(String resourceUri) throws TransmissionException {
        String reqPath = "/services/publish/publish.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);

        try {
            JSONArray jaPublished = getSuccessPayload(jtRetrieved).getJSONArray("published");

            return jaPublished.length() == 1 && jaPublished.getJSONObject(0).getString("uri").equals(resourceUri);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /**
     * /services/publish/unpublish.json
     * 
     * @param resourceUri The resource URI to unpublish
     * @return True if success, False otherwise
     * @throws TransmissionException
     */
    @Override
    public boolean unpublish(String resourceUri) throws TransmissionException {
        String reqPath = "/services/publish/unpublish.json";
        NameValuePair argRes = new BasicNameValuePair("uri", resourceUri);
        JSONTokener jtRetrieved = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argRes);

        try {
            JSONArray jaUnpublished = getSuccessPayload(jtRetrieved).getJSONArray("unpublished");

            return jaUnpublished.length() == 1 && jaUnpublished.getString(0).equals(resourceUri);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /////////
    //Execution
    ///////////

    /**
     * @throws NotImplementedException
     */
    @Override
    public String runFlow(String flowUri, boolean verbose) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    public String runFlow(String flowUri, HashMap<String,String> probeList) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public String runRepository(Model model) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public InputStream runFlowStreamOutput(String flowUri, boolean verbose) throws TransmissionException {
        return runFlowStreamOutput(flowUri, null, verbose);
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public InputStream runFlowStreamOutput(String flowUri, String token, boolean verbose) throws TransmissionException {
        throw new NotImplementedException();
    }
    
    /**
     * /services/jobs/submit.json
     * 
     * @param flowUri The flow URI to run
     * @return The job id
     * @throws TransmissionException
     */
    public String submitJob(String flowUri) throws TransmissionException {
        String reqPath = "/services/jobs/submit.json";
        
        NameValuePair argFlowUri = new BasicNameValuePair("uri", flowUri);
        JSONTokener jtResponse = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argFlowUri);
        
        try {
            JSONArray jaSubmitted = getSuccessPayload(jtResponse).getJSONArray("submitted");
            
            return jaSubmitted.getJSONObject(0).getString("jobID");
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return null;
        }
    }
    
    /**
     * /services/jobs/list.json
     * 
     * @param jobID The job id
     * @return The job status
     * @throws TransmissionException
     */
    public String retrieveJobStatus(String jobID) throws TransmissionException {
        String reqPath = "/services/jobs/list.json";
        
        NameValuePair argJobId = new BasicNameValuePair("jobID", jobID);
        JSONTokener jtResponse = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argJobId);
        
        try {
            JSONArray jaJobs = getSuccessPayload(jtResponse).getJSONArray("jobs");
            JSONArray jaProgress = jaJobs.getJSONObject(0).getJSONArray("progress");
            
            return jaProgress.getJSONObject(jaProgress.length() - 1).getString("status");
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return null;
        }
    }
    
    /**
     * /services/jobs/kill.json
     * 
     * @param jobID The job id
     * @return True if request successful, False otherwise
     * @throws TransmissionException
     */
    public boolean killJob(String jobID) throws TransmissionException {
        String reqPath = "/services/jobs/kill.json";
        
        NameValuePair argJobId = new BasicNameValuePair("jobID", jobID);
        JSONTokener jtResponse = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance(), argJobId);
        
        try {
            return getSuccessPayload(jtResponse).getJSONArray("kill").length() == 1;
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public JSONObject retrieveWebUIInfo(String token) throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    public Map<URI,URI> retrieveRunningFlows() throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    public Map<URI,Map<String,URI>> retrieveRunningFlowsInformation() throws TransmissionException{
        throw new NotImplementedException();
    }

    /**
     * @throws NotImplementedException
     */
    @Override
    public Vector<Map<String,String>> retrieveJobStatuses() throws TransmissionException {
        throw new NotImplementedException();
    }

    /**
     * /services/jobs/console.txt
     * 
     * @param jobID The job id
     * @return The job console
     * @throws TransmissionException
     */
    @Override
    public String retrieveJobConsole(String jobID) throws TransmissionException {
        String reqPath = "/services/jobs/console.txt";
        
        NameValuePair argJobID = new BasicNameValuePair("jobID", jobID);
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance(), argJobID);
    }

    ////////////
    //Public
    //////////////

    /**
     * /public/services/repository.nt
     * 
     * @return A QueryableRepository for the public repository
     * @throws TransmissionException
     */
    @Override
    public QueryableRepository retrievePublicRepository() throws TransmissionException {
        String reqPath = "/public/services/repository.nt";
        InputStream modelStream = _httpClient.doGET(reqPath, null);

        Model model = ModelFactory.createDefaultModel();
        model.read(modelStream, null, "N-TRIPLE");
        
        return new RepositoryImpl(model);
    }

    /**
     * /public/services/demo_repository.nt
     * 
     * @return A QueryableRepository for the demo repository
     * @throws TransmissionException
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

    /** 
     * /services/repository/describe.nt
     *
     * @param componentUri The component URI
     * @return The RDF descriptor of the component
     * @throws TransmissionException
     */
    public String retrieveComponentDescriptorAsString(String componentUri) throws TransmissionException {
        String reqPath = "/services/repository/describe.nt";
        NameValuePair argCompUri = new BasicNameValuePair("uri", componentUri);
        
        return _httpClient.doGET(reqPath, null, StringResponseHandler.getInstance(), argCompUri);
    }

    /** 
     * FIXME: Needs proper implementation and server-side support
     */
    @Override
    public String getServerVersion() throws TransmissionException {
        return "version = 2.0.0";
    }

    /** 
     * @throws NotImplementedException
     */
    public String getServerPlugins() throws TransmissionException {
        throw new NotImplementedException();
    }

    /** 
     * @throws NotImplementedException
     */
    public String getComponentJarInfo(String jarFile) throws TransmissionException {
        throw new NotImplementedException();
    }

    /** 
     * /public/services/ping.json
     *
     *  @return True if it successfully pinged the server
     *  @throws TransmissionException
     */
    @Override
    public boolean ping() throws TransmissionException {
        String reqPath = "/public/services/ping.json";
        
        JSONTokener jtResponse = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        try {
            return getSuccessPayload(jtResponse).has("message");
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
        catch (OperationFailedException e) {
            return false;
        }
    }
    
    private JSONObject getSuccessPayload(JSONTokener response) throws JSONException, OperationFailedException {
        JSONObject joResponse = new JSONObject(response);
        String status = joResponse.getString("status");
        
        if (status.equals("OK"))
            return joResponse.getJSONObject("success");
        
        if (status.equals("FAIL")) {
            JSONObject joFailure = joResponse.getJSONObject("failure");
            throw new OperationFailedException(joResponse.getString("message"), joFailure);
        }
        
        throw new JSONException("Invalid response status: " + status);
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
