/**
 * MeandreProxy creates and maintains a local cache of a remote
 * Meandre Repository.
 */

package org.meandre.tools.client.v1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.LocationBean;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.tools.client.exceptions.TransmissionException;
import org.meandre.tools.client.utils.GenericLoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;



public class MeandreProxy extends MeandreClient {

	/** The logger we'll write to */
	protected Logger log = null;

    /** the meandre client to handle the calls to the server */
    private MeandreClient client;

	/** The user name */
	private String sUserName;

	/** The password */
	private String sPassword;

	/** The base url of the remote server */
	private String sBaseURL;

	/** The credentials */
	@SuppressWarnings("unused")
	private String sUPEncoding;

	/** Cached roles */
	private Set<String> mapRoles;

	/** Cached repository */
	private QueryableRepository qrCached;

	/** Is the proxy ready? */
	private boolean bIsReady;

	/** Did the last call succeed */
	private boolean bWasCallOK;

	/**Server version string*/
	private JSONObject serverVersion;

	/** Creates an empty Meandre Proxy
	 */
	public MeandreProxy () {
		super("",0);
		bIsReady = bWasCallOK = false;
        //set logger to default client logger
        setLogger(GenericLoggerFactory.getLogger());
		qrCached = new RepositoryImpl(ModelFactory.createDefaultModel());
	}

	/** Creates a Meandre Proxy and contacts the server to initialize
     * the cache.
	 *
	 * @param sUser The user of the proxy
	 * @param sPasswd The password of the proxy
	 * @param sServerHost The Meandre server
	 * @param iServerPort The Meandre server port
	 */
	public MeandreProxy ( String sUser, String sPasswd, String sServerHost,
            int iServerPort ) {
		super(sUser,iServerPort);
        //set logger to default client logger
        setLogger(GenericLoggerFactory.getLogger());
		update(sUser,sPasswd,sServerHost,iServerPort);
	}

	/**Call this function when you want to reuse the function
	 *
	 * @param sUser The user of the proxy
	 * @param sPasswd The password of the proxy
	 * @param sServerHost The Meandre server
	 * @param iServerPort The Meandre server port
	 */
	public void update ( String sUser, String sPasswd, String sServerHost,
			int iServerPort ) {
		this.sUserName = sUser;
		this.sPassword = sPasswd;

		String hostWithProtocol = "http://"+sServerHost;
		this.sBaseURL  = hostWithProtocol +":"+iServerPort +"/";

		this.client = new MeandreClient(sServerHost, iServerPort);
        client.setCredentials(sUser, sPasswd);

		String sUserPassword = sUserName + ":" + sPassword;
		this.sUPEncoding = new String(Base64.encodeBase64(sUserPassword.getBytes()));

		// Force a first authetication for role caching
		this.bIsReady = null!=getRoles();
		// Force the repository caching
		this.qrCached = getRepository();
	}

	/** Returns true if the proxy was successfully initialized; false otherwise.
	 *
	 * @return True is successfully initialized
	 */
	public boolean isReady() {
		return bIsReady;
	}

	/** Returns true if the last call was completed successfully.
	 *
	 * @return True if everything when well. False otherwise
	 */
	public boolean getCallOk () {
		return bWasCallOK;
	}

	/** Gets the user name.
	 *
	 * @return The user name
	 */
	public String getName () {
		return sUserName;
	}

	/** Flushes the cached roles.
	 *
	 */
	public void flushRoles () {
		mapRoles = null;
	}



	/** Flushes the cached repository.
	 *
	 */
	public void flushRepository () {
		qrCached = null;
	}

	/** Return the roles for the user of this proxy.
	 *
	 * @return The set of granted role for the proxy user
	 */
	public Set<String> getRoles() {
		if ( mapRoles==null ) {
            try{
                //Set<String> roles = this.client.retrieveUserRoles();
                this.mapRoles=this.client.retrieveUserRoles();
                bWasCallOK = true;
            }catch(TransmissionException e){
                bWasCallOK = false;
                log("Couldn't retrieve roles: " + e.toString());
            }
        }
		return mapRoles;
	}

	/** Gets the current cached repository.
	 *
	 * @return The cached queryable repository
	 */
	public QueryableRepository getRepository () {
		if ( this.qrCached==null ) {
            try{
                this.qrCached = this.client.retrieveRepository();
                bWasCallOK = true;
            }catch(TransmissionException e){
                bWasCallOK = false;
                log("Couldn't retrieve Repository: " +e.toString());
            }

        }
		return this.qrCached;
	}



	/** Retrieves the public repository from the server (no cacheing).
	 *
	 * @return The public queryable repository
	 */
	public QueryableRepository getPublicRepository () {
		// The public repository
        try{
            QueryableRepository qr = this.client.retrievePublicRepository();
            bWasCallOK = true;
            return qr;
        }catch(TransmissionException e){
            bWasCallOK = false;
            log("Couldn't retrieve Public Repository: " + e.toString());
        }
        return null;
	}


	/** Forces the repository to be recached before returning it.
	 *
	 * @return The recached repository
	 */
	public QueryableRepository getRepositoryFlush () {
		this.qrCached = null;
		return getRepository();
	}

	/** Return the list of locations for the user of this proxy.
	 *
	 * @return The array of location for this user
	 */

	public LocationBean[] getLocations() {
		bWasCallOK = true;
		Set<LocationBean> loca = null;
        try{
            loca = this.client.retrieveLocations();
            bWasCallOK = true;
        }catch(TransmissionException e){
            bWasCallOK = false;
            log("Couldn't retrieve locations: " + e.toString());
        }
        LocationBean[] locArray = new LocationBean[loca.size()];
        return loca.toArray(locArray);
	}

	/** regenerates the  remote user repository and updates the local cache.
	 *
	 * @return The result of the process. true if succesfull
	 */
	public boolean getRegenerate () {
		boolean localWasCallOK = true;

        try{
            localWasCallOK = this.client.regenerate();
            bWasCallOK = true;
        }catch(TransmissionException e){
    		localWasCallOK = false;
            log("Proxy couldn't regenerate repository:") ;
        }
		getRepositoryFlush();

        //set was call ok to true only if the local regenerate and the
        //repository flush both succeeded.
		bWasCallOK = localWasCallOK && bWasCallOK;
		return bWasCallOK;
	}

	/** Gets the result of attempting to add a new location to the user repository.
	 *
	 * @param sLocation The URL location
	 * @param sDescription The location description
	 * @return The result of the process. True if it was succesful
	 */
	public boolean getAddLocation (String sLocation, String sDescription ) {
        bWasCallOK = true;
        try{
            if ( mapRoles!=null ) {
                bWasCallOK = this.client.addLocation(sLocation, sDescription);
                bWasCallOK = true;
            }
        }catch(Exception e){
            bWasCallOK = false;
            log("Proxy couldn't add location:" + e.toString());
        }
        return bWasCallOK;
    }


	/** Gets the result of attempting to remove a location from the user repository.
	 *
	 * @param sLocation The URL location
	 * @return true if the removal was successful
	 */
	public boolean getRemoveLocation (String sLocation ) {
        bWasCallOK = true;
        try{
            if ( mapRoles!=null ) {
                bWasCallOK = this.client.removeLocation(sLocation);
                bWasCallOK = true;
            }
        }catch(Exception e){
            bWasCallOK = false;
            log("Proxy couldn't remove location:" + e.toString());
        }
        return bWasCallOK;
	}

	/** publishes a component or flow (identified by it's uri) at the remote
     * server.
	 *
	 * @param sURI The resource URI to publish
	 * @return The result of the process. Returns true if successful
     **/
	public boolean getPublish (String sURI ) {
        if ( mapRoles!=null ) {
		    try {
			    bWasCallOK = this.client.publish(sURI);
                bWasCallOK = true;
			} catch (TransmissionException e) {
                bWasCallOK = false;
			    log.warning("Proxy couldn't perform publish: " + e);
			}
		}
		return bWasCallOK;
	}

	/** unpublishes a component or flow (identified by it's uri) at the remote
     * server.
     *
     * returns true no matter what as long as the server received and understood
     * the request.
	 *
	 * @param sURI The resource URI to publish
	 * @return The result of the process. Returns true if successful
     **/
	public boolean getUnpublish (String sURI ) {
        if (mapRoles!=null) {
		    try {
			    bWasCallOK = this.client.unpublish(sURI);
                bWasCallOK = true;
			} catch (TransmissionException e) {
                bWasCallOK = false;
			    log.warning("Proxy couldn't perform unpublish: " + e);
			}
		}
		return bWasCallOK;
	}



	/** Gets the result of attempting to remove a component or flow, identified
     * by it's, URI from the user repository.
	 *
	 * @param sURI The resource URI to remove
	 * @return  true if successful
	 */
	public boolean getRemove (String sURI ) {
		if ( mapRoles!=null ) {
			try {
				bWasCallOK = this.client.removeResource(sURI);
                bWasCallOK = true;
			} catch (TransmissionException e) {
                bWasCallOK = false;
			    log.warning("Proxy couldn't perform remove: " + e);
			}
            if(bWasCallOK){
				flushRepository();
            }
		}
		return bWasCallOK;
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
    public Set<Map<String,URI>> getRunningFlowsInformation() {
    	try {
    		Set<Map<String,URI>> setRes = new HashSet<Map<String,URI>>(10);
    		Map<URI,Map<String,URI>> mapTmp = this.client.retrieveRunningFlowsInformation();
			for ( URI uri:mapTmp.keySet() )
				setRes.add(mapTmp.get(uri));
            bWasCallOK = true;
			return setRes;
		} catch (TransmissionException e) {
            bWasCallOK = false;
			return new HashSet<Map<String,URI>>();
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
    public Vector<Map<String,String>> getJobStatuses() {
    	try {
	        Vector<Map<String, String>> js = this.client.retrieveJobStatuses();
            bWasCallOK = true;
            return js;
    	}
    	catch ( Exception e ) {
            bWasCallOK = false;
    		return new Vector<Map<String,String>>();
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
    public String getJobConsole(String sFUID) {
    	try {
	        String jc = this.client.retrieveJobConsole(sFUID);
            bWasCallOK = true;
            return jc;
    	}
    	catch ( Exception e ) {
            bWasCallOK = false;
    		return "Console not available";
    	}
    }

    /** Runs the requested model on the server
     *
     * @param mod The model to run
     * @return The output
     */
    @Override
    public String runRepository (Model mod) {
    	try {
			return client.runRepository(mod);
		} catch (TransmissionException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);
			return "Failed to run the requested repository!!!\n"+baos.toString();
		}
    }

	/** Return the list of running flows of this proxy.
	 *
     *
	 * @return The set of running flows. will return an empty set if
     * the transmission failed, so check wasCallOK()
	 */
	/*public Set<RunningFlow> getRunningFlows() {
        Set<RunningFlow> flowBeans = new HashSet<RunningFlow>();

		if ( mapRoles!=null ) {
            try{
                Map<URL, URL> flowMap= this.client.retrieveRunningFlows();

                Iterator<URL> keyIter = flowMap.keySet().iterator();
                while(keyIter.hasNext()){
                    URL key = keyIter.next();
                    URL val = flowMap.get(key);
                    RunningFlow flow = new RunningFlow(
                            val.toString(), key.toString());
                    flowBeans.put(flow);
                }
            }catch(TransmissionException e){
                bWasCallOK = false;
                log("Proxy couldn't retrieve running flows: " + e.toString());
            }
		}
		return flowBeans;
	}*/


    /**
     * sets the logger for warning and error messages. some log messages will
     * still go to standard out.
     */
    @Override
    public void setLogger(Logger newLogger){
        log = newLogger;
    }

    @Override
    public Logger getLogger(){
        return log;
    }



	/** Runs a flow and streams the output.
	 *
	 * @param sURI The flow to execute
	 * @param sFormat The format of the output
	 * @param jw The writer to use
	 */
	/*public void runFlowInteractively ( String sURI, String sFormat, JspWriter jw ) {
		String sRequest = sBaseURL+"services/execute/flow."+sFormat+"?statistics=true&uri="+sURI;
		executeStreamableGetRequest(sRequest,jw);
	}*/

	/**
	 * handle generic logging messages for this proxy's default logging level
	 * @param msg
	 */
	private void log(String msg){
	    System.out.println(msg);
	}

	/** Does an authenticated get request against the provided URL and stream back
	 * the contents
	 *
	 * @param sURL The URL to request
	 * @param jw The outpt writter
	 */
	/*private void executeStreamableGetRequest(String sURL, JspWriter jw) {
		try {
			// Create the URL
			URL url = new URL(sURL);

			// Create and authenticated connection
			URLConnection uc = url.openConnection();
			uc.setRequestProperty ("Authorization", "Basic " + sUPEncoding);

			// Pull the stuff out of the Meandre server
			InputStream is = (InputStream)uc.getInputStream();
			int iTmp;
			while ( (iTmp=is.read())!=-1 )
				jw.write(iTmp);

			is.close();
		}
		catch ( IOException e ) {
			log.warning(e.toString());
		}
	}*/

	// ---- Amit's patch comented by Xavier ---------------
	// TODO: Write the proper test for these methods

    /** Gets the RDF component description
	 *
     * @param componentUri The component URI
     * @return The string containing the RDF
     */
	public String getComponentDescriptor(String componentUri) {
		String descriptor=null;
		try {
			descriptor = this.client.retrieveComponentDescriptorAsString(componentUri);
		} catch (TransmissionException e) {
			log.severe(e.getMessage());
		}
		return descriptor;
	}


	/** Gets the server version.
	 *
	 * @return The server version
	 * @throws TransmissionException Could not get the server version
	 */
	@Override
    public String getComponentJarInfo(String jarFile) {
		String jarInfo=null;
		try {
			jarInfo=this.client.getComponentJarInfo(jarFile);
		} catch (TransmissionException e) {
			log.severe(e.getMessage());
		}
		return jarInfo;
	}

	/** Pings the server
	 *
	 *	@return True if it successfully pinged the server
	 */
	@Override
    public boolean ping() {
		try {
			return this.client.ping();
		} catch (TransmissionException e) {
			log.severe(e.getMessage());
		}
		return false;
	}

	/** Gets the server version.
	 *
	 * @return The server version
	 * @throws TransmissionException Could not get the server version
	 */
	@Override
    public JSONObject getServerVersion() {
		JSONObject version = null;
		int status= 500;
		try {
			version = this.client.getServerVersion();
		} catch (TransmissionException e) {
			log.severe(e.getMessage());
		}
		try {
			if (version==null || status == 404) {
				bWasCallOK = false;
				this.serverVersion = new JSONObject().put("version", "unknown");
			} else {
				if (!version.has("version")) {
					log.warning("Error could not get the server version");
					this.serverVersion = new JSONObject().put("version", "unknown");
					return this.serverVersion;
				}
				this.serverVersion = version;
			}
		}
		catch (JSONException e) {}

		return this.serverVersion;
	}

	/** Return the JSON content describing the plugins available.
	 *
	 * @return The JSON string
	 * @throws TransmissionException Fail to retrieve the plugins' information
	 */
	public String getServerPluginsAsJSON() {
		String pluginString = null;
		try {
			pluginString = this.client.getServerPlugins();
		} catch (TransmissionException e) {
			log.fine(e.getMessage());
		}
		return pluginString;
	}

    public String getUserName() {
        return sUserName;
    }

    public void setUserName(String userName) {
        sUserName = userName;
    }

    public String getPassword() {
        return sPassword;
    }

    public void setPassword(String password) {
        sPassword = password;
    }

    public String getBaseURL() {
        return sBaseURL;
    }

    public void setBaseURL(String baseURL) {
        sBaseURL = baseURL;
    }

}
