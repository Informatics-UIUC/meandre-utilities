package org.meandre.tools.client;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
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
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.tools.client.exceptions.OperationFailedException;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.meandre.tools.client.utils.GenericHttpClient;
import org.meandre.tools.client.utils.handlers.JSONResponseHandler;
import org.meandre.tools.client.utils.handlers.RDFModelResponseHandler;
import org.seasr.meandre.support.generic.io.ModelUtils;
import org.seasr.meandre.support.generic.util.KeyValuePair;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * @author Boris Capitanu
 */
public class SCClient {

    protected final GenericHttpClient _httpClient;
    protected final Credentials _credentials;
    
    public SCClient(HttpHost host) {
        this(host, null);
    }
    
    public SCClient(HttpHost host, Credentials credentials) {
        _httpClient = new GenericHttpClient(host);
        _httpClient.setCredentials(credentials);
        _credentials = credentials;
    }

    public void close() {
        _httpClient.close();
    }
    
    // Components
    public JSONArray listSharedGroupComponents(String groupName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups/%s/components.json", groupName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listSharedUserComponents(String userName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/components.json", userName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public InputStream getComponentContext(SCComponent component, String md5) throws TransmissionException {
        String reqPath = String.format("/services/components/%s/versions/%d/contexts/%s", component.getComponentId(), component.getVersion(), md5);
        return _httpClient.doGET(reqPath, null);
    }
    
    public JSONObject getComponentMetadata(SCComponent component) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/components/%s/versions/%d.json", component.getComponentId(), component.getVersion());
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult).getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public ExecutableComponentDescription getComponent(SCComponent component) throws TransmissionException {
        String reqPath = String.format("/services/components/%s/versions/%d.nt", component.getComponentId(), component.getVersion());
        Model compModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(compModel).getAvailableExecutableComponentDescriptions().iterator().next();
    }
    
    public Set<ExecutableComponentDescription> getSharedGroupComponents(String groupName) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/components.nt", groupName);
        Model compsModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(compsModel).getAvailableExecutableComponentDescriptions();
    }
    
    public Set<ExecutableComponentDescription> getSharedUserComponents(String userName) throws TransmissionException {
        String reqPath = String.format("/services/users/%s/components.nt", userName);
        Model compsModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(compsModel).getAvailableExecutableComponentDescriptions();
    }
    
    public JSONObject shareComponentWithGroup(String groupName, SCComponent component) throws TransmissionException, OperationFailedException {
        SCResponse response = shareComponentsWithGroup(groupName, component);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse shareComponentsWithGroup(String groupName, SCComponent ... components) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/components.json", groupName);
        NameValuePair[] nvps = new NameValuePair[components.length * 2];
        for (int i = 0, iMax = nvps.length; i < iMax; i += 2) {
            SCComponent component = components[i / 2];
            nvps[i] = new BasicNameValuePair("component", component.getComponentId().toString());
            nvps[i+1] = new BasicNameValuePair("version", component.getVersion().toString());
        }
        
        try {
            return parseResponse(_httpClient.doPOST(reqPath, null, null, JSONResponseHandler.getInstance(), nvps));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject uploadComponent(String userName, Model model, File... contexts) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/components.json", userName);
        List<KeyValuePair<String, ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();
        
        try {
            parts.add(new KeyValuePair<String, ContentBody>("component_rdf", new StringBody(ModelUtils.modelToDialect(model, "N-TRIPLE"))));
            for (File context : contexts)
                parts.add(new KeyValuePair<String,ContentBody>("context", new FileBody(context)));
    
            JSONTokener jtResponse = _httpClient.doPOST(reqPath, null, parts, JSONResponseHandler.getInstance());
            return getSuccessPayload(jtResponse).getJSONObject(0);
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    // Flows
    public JSONArray listSharedGroupFlows(String groupName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups/%s/flows.json", groupName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listSharedUserFlows(String userName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/flows.json", userName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());
        
        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject getFlowMetadata(SCFlow flow) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/flows/%s/versions/%d.json", flow.getFlowId(), flow.getVersion());
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult).getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public FlowDescription getFlow(SCFlow flow) throws TransmissionException {
        String reqPath = String.format("/services/flows/%s/versions/%d.nt", flow.getFlowId(), flow.getVersion());
        Model flowModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(flowModel).getAvailableFlowDescriptions().iterator().next();
    }
    
    public Set<FlowDescription> getSharedGroupFlows(String groupName) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/flows.nt", groupName);
        Model flowsModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(flowsModel).getAvailableFlowDescriptions();
    }
    
    public Set<FlowDescription> getSharedUserFlows(String userName) throws TransmissionException {
        String reqPath = String.format("/services/users/%s/flows.nt", userName);
        Model flowsModel = _httpClient.doGET(reqPath, null, RDFModelResponseHandler.getInstance());
        
        return new RepositoryImpl(flowsModel).getAvailableFlowDescriptions();
    }
    
    public JSONObject shareFlowWithGroup(String groupName, SCFlow flow) throws TransmissionException, OperationFailedException {
        SCResponse response = shareFlowsWithGroup(groupName, flow);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse shareFlowsWithGroup(String groupName, SCFlow ... flows) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/flows.json", groupName);
        NameValuePair[] nvps = new NameValuePair[flows.length * 2];
        for (int i = 0, iMax = nvps.length; i < iMax; i += 2) {
            SCFlow flow = flows[i / 2];
            nvps[i] = new BasicNameValuePair("flow", flow.getFlowId().toString());
            nvps[i+1] = new BasicNameValuePair("version", flow.getVersion().toString());
        }
        
        try {
            return parseResponse(_httpClient.doPOST(reqPath, null, null, JSONResponseHandler.getInstance(), nvps));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject uploadFlow(String userName, Model model) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/flows.json", userName);
        List<KeyValuePair<String, ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();
        
        try {
            parts.add(new KeyValuePair<String, ContentBody>("flow_rdf", new StringBody(ModelUtils.modelToDialect(model, "N-TRIPLE"))));
            
            JSONTokener jtResponse = _httpClient.doPOST(reqPath, null, parts, JSONResponseHandler.getInstance());
            return getSuccessPayload(jtResponse).getJSONObject(0);
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    // Groups
    public JSONObject addGroupMember(String groupName, SCGroupUser user) throws TransmissionException, OperationFailedException {
        SCResponse response = addGroupMembers(groupName, user);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse addGroupMembers(String groupName, SCGroupUser ... users) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/members.json", groupName);
        NameValuePair[] nvps = new NameValuePair[users.length * 2];
        for (int i = 0, iMax = nvps.length; i < iMax; i += 2) {
            SCGroupUser user = users[i / 2];
            nvps[i] = new BasicNameValuePair("user", user.getUserId());
            nvps[i+1] = new BasicNameValuePair("role", user.getRole().name());
        }
        
        try {
            return parseResponse(_httpClient.doPOST(reqPath, null, null, JSONResponseHandler.getInstance(), nvps));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject addPendingGroupMember(String groupName, String userName) throws TransmissionException, OperationFailedException {
        SCResponse response = addPendingGroupMembers(groupName, userName);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse addPendingGroupMembers(String groupName, String ... userNames) throws TransmissionException {
        String reqPath = String.format("/services/groups/%s/members/pending.json", groupName);
        NameValuePair[] nvps = new NameValuePair[userNames.length];
        for (int i = 0, iMax = nvps.length; i < iMax; i ++)
            nvps[i] = new BasicNameValuePair("user", userNames[i]);
        
        try {
            return parseResponse(_httpClient.doPOST(reqPath, null, null, JSONResponseHandler.getInstance(), nvps));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject createGroup(String userName, SCGroup group) throws TransmissionException, OperationFailedException {
        SCResponse response = createGroups(userName, group);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse createGroups(String userName, SCGroup ... groups) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/groups.json", userName);
        List<KeyValuePair<String,ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();

        try {
            for (SCGroup group : groups) {
                parts.add(new KeyValuePair<String,ContentBody>("name", new StringBody(group.getGroupName())));
                parts.add(new KeyValuePair<String,ContentBody>("profile", new StringBody(group.getProfile().toString())));
            }
            
            return parseResponse(_httpClient.doPOST(reqPath, null, parts, JSONResponseHandler.getInstance()));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject getGroupMetadata(String groupName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups/%s.json", groupName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult).getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listSharedGroupsForComponent(SCComponent component) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/components/%s/versions/%d/groups.json", component.getComponentId(), component.getVersion());
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listSharedGroupsForFlow(SCFlow flow) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/flows/%s/versions/%d/groups.json", flow.getFlowId(), flow.getVersion());
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listGroupMembers(String groupName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups/%s/members.json", groupName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listGroups() throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups.json");
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listPendingGroupMembers(String groupName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/groups/%s/members/pending.json", groupName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listGroupsForUser(String userName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s/groups.json", userName);
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    // Roles
    public JSONArray listRoles() throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/roles.json");
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    // Users
    public JSONObject createUser(String userName, SCUser user) throws TransmissionException, OperationFailedException {
        SCResponse response = createUsers(userName, user);
        try {
            checkFailure(response.getFailure());
            return response.getSuccess().getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public SCResponse createUsers(String userName, SCUser ... users) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users.json", userName);
        List<KeyValuePair<String,ContentBody>> parts = new ArrayList<KeyValuePair<String,ContentBody>>();

        try {
            for (SCUser user : users) {
                parts.add(new KeyValuePair<String,ContentBody>("screen_name", new StringBody(user.getUserName())));
                parts.add(new KeyValuePair<String,ContentBody>("password", new StringBody(user.getPassword())));
                parts.add(new KeyValuePair<String,ContentBody>("profile", new StringBody(user.getProfile().toString())));
            }
            
            return parseResponse(_httpClient.doPOST(reqPath, null, parts, JSONResponseHandler.getInstance()));
        }
        catch (UnsupportedEncodingException e) {
            throw new TransmissionException(e);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject deleteUser(String userName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s.json");
        JSONTokener jtResult = _httpClient.doDELETE(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult).getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONArray listUsers() throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users.json");
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    public JSONObject getUserMetadata(String userName) throws TransmissionException, OperationFailedException {
        String reqPath = String.format("/services/users/%s.json");
        JSONTokener jtResult = _httpClient.doGET(reqPath, null, JSONResponseHandler.getInstance());

        try {
            return getSuccessPayload(jtResult).getJSONObject(0);
        }
        catch (JSONException e) {
            throw new TransmissionException(e);
        }
    }
    
    
    private JSONArray getSuccessPayload(JSONTokener response) throws JSONException, OperationFailedException {
        SCResponse scResponse = parseResponse(response);
        JSONArray jaSuccess = scResponse.getSuccess();
        JSONArray jaFailure = scResponse.getFailure();
        
        checkFailure(jaFailure);
        
        return jaSuccess;
    }

    private void checkFailure(JSONArray jaFailure) throws JSONException, OperationFailedException {
        if (jaFailure != null && jaFailure.length() > 0) {
            JSONObject joError = jaFailure.getJSONObject(0);
            String errMsg = joError.has("sc_error_reason") ? joError.getString("sc_error_reason") :
                joError.has("sc_exception_msg") ? joError.getString("sc_exception_msg") : null;
            throw new OperationFailedException(errMsg, joError);
        }
    }
    
    private SCResponse parseResponse(JSONTokener response) throws JSONException {
        JSONObject joResponse = new JSONObject(response);
        
        JSONArray jaSuccess = joResponse.has("SUCCESS") ? joResponse.getJSONArray("SUCCESS") : new JSONArray();
        JSONArray jaFailure = joResponse.has("FAILURE") ? joResponse.getJSONArray("FAILURE") : new JSONArray();
        
        return new SCResponse(jaSuccess, jaFailure);
    }
    
    
    public class SCResponse {
        
        protected final JSONArray _success;
        protected final JSONArray _failure;
        
        public SCResponse(JSONArray success) {
            this(success, null);
        }
        
        public SCResponse(JSONArray success, JSONArray failure) {
            _success = success;
            _failure = failure;
        }
        
        public JSONArray getSuccess() {
            return _success;
        }
        
        public JSONArray getFailure() {
            return _failure;
        }
    }
    
    public class SCComponent {
        
        protected final UUID _componentId;
        protected final int _version;
        
        public SCComponent(UUID componentId, int version) {
            _componentId = componentId;
            _version = version;
        }
        
        public UUID getComponentId() {
            return _componentId;
        }
        
        public Integer getVersion() {
            return _version;
        } 
    }
    
    public class SCFlow {
        
        protected final UUID _flowId;
        protected final int _version;
        
        public SCFlow(UUID flowId, int version) {
            _flowId = flowId;
            _version = version;
        }
        
        public UUID getFlowId() {
            return _flowId;
        }
        
        public Integer getVersion() {
            return _version;
        } 
    }
    
    public enum SCRole {

        ADMIN   (100),
        USER    (200);

        private final int _roleId;

        SCRole(int roleId) {
            _roleId = roleId;
        }

        public int getRoleId() {
            return _roleId;
        }
    }
    
    public class SCGroupUser {
        
        protected final String _userId;
        protected final SCRole _role;
        
        public SCGroupUser(String userId, SCRole role) {
            _userId = userId;
            _role = role;
        }
        
        public String getUserId() {
            return _userId;
        }
        
        public SCRole getRole() {
            return _role;
        }
    }
    
    public class SCGroup {
        
        protected final String _groupName;
        protected final JSONObject _profile;
        
        public SCGroup(String groupName, JSONObject profile) {
            _groupName = groupName;
            _profile = profile;
        }
        
        public String getGroupName() {
            return _groupName;
        }
        
        public JSONObject getProfile() {
            return _profile;
        }
    }
    
    public class SCUser {
        
        protected final String _userName;
        protected final String _password;
        protected final JSONObject _profile;
        
        public SCUser(String userName, String password, JSONObject profile) {
            _userName = userName;
            _password = password;
            _profile = profile;
        }
        
        public String getUserName() {
            return _userName;
        }
        
        public String getPassword() {
            return _password;
        }
        
        public JSONObject getProfile() {
            return _profile;
        }
    }
}
