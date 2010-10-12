
package org.meandre.client.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.meandre.client.exceptions.TransmissionException;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.LocationBean;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.core.security.Role;
import org.meandre.demo.repository.DemoRepositoryGenerator;
import org.meandre.webservices.MeandreServer;
import org.seasr.meandre.support.generic.util.NetUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Tests cases for MeandreClient. Starts up a MeandreServer and creates a
 * client that talks to it. Then calls all the remote calls the client
 * can make, one per test case.
 *
 * @author pgroves
 * Modified by Xavier Llor&agrave;s
 * Modified by Amit Kumar -added test for runFlow(String,HashMap)
 */
public class MeandreClientTest {

    private static String _serverUrl = NetUtils.getLocalHostName();
    private static int _serverPort = 1704;
    
    //made crossplatform in setupBeforeClass
    private static String _workingDir = "MeandreClientTest";

    private static String _sTestUploadJar = 
        ".." + File.separator + "data" + File.separator + "component.jar";
    
    private static String _sDemoRepo= "http://" + _serverUrl + ":" + _serverPort +
            "/public/services/demo_repository.nt";
    
    MeandreClient _meandreClient = null;
    
    
    private static MeandreServer _server = null;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("MeandreClientTest: setupBeforeClass begin");
        
        _serverUrl = NetUtils.getLocalHostName();
        _workingDir = _workingDir.replace('/', File.separatorChar);
        
        File fWorkDir = new File(_workingDir);
        if(!fWorkDir.exists()){
            fWorkDir.mkdirs();
        }
        MeandreServer.uninstall(fWorkDir);
        _server = new MeandreServer(_serverPort, _workingDir);
        log("MeandreServer initialized");
        //CoreConfiguration cnf = new CoreConfiguration(_serverPort,_workingDir);
        //_server.setCoreConfiguration(cnf);
        
        //_server = new MeandreServer(_serverPort, _workingDir);
        //_server.setCoreConfiguration(cnf);
        //log("MeandreServer uninstalled");
        _server.start(false);
        log("MeandreServer started");
        System.out.println("MeandreClientTest: setupBeforeClass end");
    }
    
    private static void log(String msg) {
        System.out.println("MeandreClient: " + msg);
        
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("MeandreClientTest: tearDownAfterClass");
        //CoreConfiguration config = null;
        //Store store = null;        
        _server.stop();
        MeandreServer.uninstall(new File(_workingDir));
        _server = null;
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {      
        System.out.println("------begin setup----------");
        _meandreClient = new MeandreClient(_serverUrl, _serverPort);
        _meandreClient.setCredentials("admin", "admin");
        
        //add the demo repository

        
        try{
             //boolean ret = _meandreClient.addLocation(_sDemoRepo, "Hello Demo");
        	_meandreClient.addLocation(_sDemoRepo, "Hello Demo");
            //_meandreClient.regenerate();
             
        }catch(TransmissionException e){
            e.printStackTrace();
        }      
        System.out.println("------end setup----------");
        return;
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        System.out.println("------begin teardown----------");
        _meandreClient.removeLocation(_sDemoRepo);
        //_meandreClient.regenerate();
        _meandreClient.close();
        System.out.println("------end teardown----------\n\n");
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveInstallationProperties()}.
     */
    @Test
    public void testRetrieveInstallationProperties() {
        JSONObject joProps = null;
        try{
        	joProps = _meandreClient.retrieveInstallationProperties();
            
         }catch(TransmissionException e){
             fail("Transmission failure: " + e.toString());
         }
         assertFalse(joProps.length()<=0);
         return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveRoles()}.
     */
    @Test
    public void testRetrieveUserRoles() {
        Set<String> roles = null;
        try{
           roles = _meandreClient.retrieveUserRoles();
           
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        //test to see that they all appear to be valid properties
        String expectedPrefix = Role.BASE_ROLE_URL;
        for(String observedRole: roles){
            assertTrue(observedRole.startsWith(expectedPrefix));
        }
        return;
    }
    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveValidRoles()}.
     */
    @Test
    public void testRetrieveValidRoles() {
        Set<String> roles = null;
        try{
           roles = _meandreClient.retrieveValidRoles();
           
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        Set<Role> expectedRoles = Role.getStandardRoles();
        Set<Role> observedRoles = new HashSet<Role>();
        for(String observedRoleUrl: roles){
            observedRoles.add(Role.fromUrl(observedRoleUrl));
        }
        assertTrue(observedRoles.equals(expectedRoles));
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveLocations()}.
     */
    @Test
    public void testRetrieveLocations() {
        Set<LocationBean> locs = null;
        
        String sUrlExpected = "http://" + _serverUrl + ":" + _serverPort;
        sUrlExpected += "/public/services/demo_repository.nt";
        
        //String sDescriptionExpected = "not the right description";
        String sDescriptionExpected = "Hello Demo";
        LocationBean lbExpected = new LocationBean(sUrlExpected, sDescriptionExpected);
        System.out.println("ExpectedLoc:");
        System.out.println(lbExpected.toString());
        
        try{
            locs = _meandreClient.retrieveLocations();
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        for(LocationBean lb: locs){
            System.out.println(lb.toString());
            System.out.println("isExpected? = " + lbExpected.equals(lb));
            
        }
        // assertTrue("! .../services/repository.nt", locs.contains(lbExpected));
        // This location has been permanently remove to avoid confusion on the users.
        assertTrue("! .../services/demo_repository.nt", locs.contains(lbExpected));
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#addLocation(java.lang.String, 
     * java.lang.String)}.
     */
    @Test
    public void testAddLocation() {
        String sNewLoc = "http://" + _serverUrl + ":" + _serverPort;
        sNewLoc += "/public/services/demo_repository.nt";
        
        boolean ret;
        try{
        	 // Location already added on setup
             ret = _meandreClient.addLocation(sNewLoc, "Hello Demo");
             assertFalse("!Demo Repo Added.", ret);
             
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }      
        return;
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#removeLocation(java.lang.String)}.
     */
    @Test
    public void testRemoveLocation() {
        String sNewLoc = "http://" + _serverUrl + ":" + _serverPort;
        sNewLoc += "/public/services/demo_repository.nt";
        boolean ret;
        try{
        	// Location already added on setup
            ret = _meandreClient.removeLocation(sNewLoc);
            assertTrue("!demo repo was removed", ret);
       }catch(TransmissionException e){
           fail("Transmission failure: " + e.toString());
       }
       return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveRepository()}.
     */
    @Test
    public void testRetrieveRepository() {
        QueryableRepository repo = null;
        try{
            repo = _meandreClient.retrieveRepository();
            Model mod = repo.getModel();
            StringWriter sw = new StringWriter();
            mod.write(sw);
            System.out.println(sw.toString());
            
            Set<FlowDescription> fds = repo.getAvailableFlowDescriptions();
            for(FlowDescription fd: fds){
                System.out.println(fd.getName());
            }
        }catch (TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        //TODO: test what's in the repository?
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#regenerate()}.
     */
    @Test
    public void testRegenerate() {
        try{
            boolean ret = _meandreClient.regenerate();
            assertTrue("!regeneration success", ret);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveComponentUrls()}.
     */
    @Test
    public void testRetrieveComponentUris() {
        Set<URI> compUris = null;
        try{
            compUris = _meandreClient.retrieveComponentUris();
        }catch (TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        try{
        	URI expected1 = new URI("meandre://test.org/component/push-string");
            URI expected2 = new URI("meandre://test.org/component/print-object");
            /*for(URL comp: compUrls){
                System.out.println(comp.toString());
            }*/
            assertTrue("!contains 1", compUris.contains(expected1));
            assertTrue("!contains 2", compUris.contains(expected2));

        }catch(Exception e){
            e.printStackTrace();
        }
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveFlowUrls()}.
     * 
     *
     */
    @Test
    public void testRetrieveFlowUris() {
        Set<URI> flowUris = null;
        String sUri = "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        try{
            flowUris = _meandreClient.retrieveFlowUris();
        }catch (TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        try{
            URI expected = new URI(sUri);
            assertTrue("!has hello world", flowUris.contains(expected));
        }catch(Exception e){
            e.printStackTrace();
        }
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveAllTags()}.
     */
    @Test
    public void testRetrieveAllTags() {
        Set<String> tags = null;
        try{
            tags = _meandreClient.retrieveAllTags();
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        
        assertTrue("!has string", tags.contains("string"));
        assertTrue("!has print", tags.contains("print"));        
        assertTrue("!has concatenate", tags.contains("concatenate"));
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveComponentTags()}.
     */
    @Test
    public void testRetrieveComponentTags() {
        Set<String> tags = null;
        try{
            tags = _meandreClient.retrieveComponentTags();
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        assertTrue("!has string", tags.contains("string"));
        assertTrue("!has print", tags.contains("print"));        
        assertTrue("!has concatenate", tags.contains("concatenate"));       
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveFlowTags()}.
     */
    @Test
    public void testRetrieveFlowTags() {
        Set<String> tags = null;
        try{
            tags = _meandreClient.retrieveFlowTags();
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }     
        assertTrue("!has string", tags.contains("hello_world"));
        assertTrue("!has print", tags.contains("demo"));        
        assertFalse("has concatenate", tags.contains("concatenate"));
        return;
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveComponentsByTag(
     * java.lang.String)}.
     */
    @Test
    public void testRetrieveComponentsByTag() {
        //test retrieving "PrintObject" component with tag "print"
        String tag = "print";
        URI expectedCompUrl = null;
        try{
            expectedCompUrl = new URI("meandre://test.org/component/print-object");
        }catch(Exception e){
            fail("uri prob");
        }
        Set<URI> compUris = null;
        try{
            compUris = _meandreClient.retrieveComponentsByTag(tag);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }     
        assertEquals(1, compUris.size());
        assertTrue(compUris.contains(expectedCompUrl));
        
        //test doing a retrieve when there are no components for a tag
        tag = "lkjdklwkeknc";
        try{
            compUris = _meandreClient.retrieveComponentsByTag(tag);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        assertEquals(0, compUris.size());
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveFlowsByTag(java.lang.String)}.
     */
    @Test
    public void testRetrieveFlowsByTag() {
        
        //test retrieving "hello world" by "demo"
        String tag = "demo";
        URI expectedFlowUri = null;
        try{
            String sUri = "meandre://test.org/flow/";
            sUri += "test-hello-world-with-python-and-lisp/";
            expectedFlowUri = new URI(sUri);
        }catch(Exception e){
            fail("url prob");
        }
        Set<URI> flowUris = null;
        try{
            flowUris = _meandreClient.retrieveFlowsByTag(tag);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }     
        assertEquals(1, flowUris.size());
        assertTrue(flowUris.contains(expectedFlowUri));   
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveComponentDescriptor(
     * java.lang.String)}.
     */
    @Test
    public void testRetrieveComponentDescriptor() {
        String sUri = "meandre://test.org/component/print-object";
        ExecutableComponentDescription comp = null;
        try{
            comp = _meandreClient.retrieveComponentDescriptor(sUri);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        assertEquals(sUri, comp.getExecutableComponentAsString());
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveFlowDescriptor(java.lang.String)}.
     */
    @Test
    public void testRetrieveFlowDescriptor() {
        String sUri = "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        FlowDescription comp = null;
        try{
            comp = _meandreClient.retrieveFlowDescriptor(sUri);
        }catch(TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        System.out.println(comp.getTags().toString());
        assertEquals(sUri, comp.getFlowComponentAsString());
    }

    /**
     * Test method for 
     * {@link org.meandre.client.MeandreClient#retrieveComponentUrlsByQuery(
     * java.lang.String)}.
     */
    /*@Test
    public void testRetrieveComponentUrlsByQuery() {
        fail("Not yet implemented");
    }*/

    /**
     * Test method for 
     * {@link org.meandre.client.MeandreClient#retrieveFlowUrlsByQuery(
     * java.lang.String)}.
     */
    /*@Test
    public void testRetrieveFlowUrlsByQuery() {
        fail("Not yet implemented");
    }*/

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#upload(byte[][], 
     * boolean, boolean, com.hp.hpl.jena.rdf.model.Model)}.
     */
    @Test
    public void testUploadComponent() {
        Set<File> jars = new HashSet<File>();
        File jarFile = new File(_sTestUploadJar);
        jars.add(jarFile);
        //assertTrue("FileExists", jarFile.exists());
        System.out.println(jarFile.toString());
        Model modRepo = DemoRepositoryGenerator.getTestHelloWorldRepository();
        RepositoryImpl repo = new RepositoryImpl(modRepo);
        for(Resource comp: 
                repo.getAvailableExecutableComponents()){
            System.out.println(comp.toString());
        }
        String sCompUrl = "meandre://test.org/component/print-object";
        Resource rCompUrl = ResourceFactory.createResource(sCompUrl);
        ExecutableComponentDescription comp = 
                repo.getExecutableComponentDescription(rCompUrl);
        try{
            boolean ret;
            //ret = _meandreClient.removeResource(sCompUrl);
            ret = _meandreClient.uploadComponent(comp, jars, true);
            assertTrue("!upload success", ret);
        }catch(Exception e){
            e.printStackTrace();
            fail("could not complete upload");
        }
        return;
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#uploadFlow(FlowDescription, boolean)}.
     */
    @Test
    public void testUploadFlow() {
        Model modTestRepo = DemoRepositoryGenerator.getTestHelloWorldRepository();
        QueryableRepository repo = new RepositoryImpl(modTestRepo);
        Set<FlowDescription> flows = repo.getAvailableFlowDescriptions();
        Iterator<FlowDescription> iter = flows.iterator();
        FlowDescription flow = iter.next();
        String sResourceUrl = flow.getFlowComponentAsString();
        System.out.println(sResourceUrl);
        boolean retStat = false;
        Set<URI> flowsAfter = null;
        try{
            retStat = _meandreClient.uploadFlow(flow, true);
            flowsAfter = _meandreClient.retrieveFlowUris();
        }catch(Exception e){
            fail("TransmissionFailure: " + e.toString());
        }
        System.out.println("Retrieved Flows:");
        for(URI flowUri: flowsAfter){
            System.out.println(flowUri.toString());
        }
        assertTrue("!upload return code", retStat);
        try{
        assertTrue("!Contains resource URI", 
                   flowsAfter.contains(new URI(sResourceUrl)));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Test method for 
     * {@link org.meandre.client.MeandreClient#uploadFiles(Set<File>, boolean)}.
     */
    @Test
    public void testUploadFiles() {
        Set<File> jars = new HashSet<File>();
        File jarFile = new File(_sTestUploadJar);
        jars.add(jarFile);
        //assertTrue("FileExists", jarFile.exists());
        System.out.println(jarFile.toString());
    
        try{
            boolean ret;
            //ret = _meandreClient.removeResource(sCompUrl);
            ret = _meandreClient.uploadFiles(jars, true);
            assertTrue("!upload success", ret);
        }catch(Exception e){
            e.printStackTrace();
            fail("could not complete upload");
        }
        return;
    }
    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#remove(java.lang.String)}.
     */
    @Test
    public void testRemove() {
        Model modTestRepo = DemoRepositoryGenerator.getTestHelloWorldRepository();
        QueryableRepository repo = new RepositoryImpl(modTestRepo);
        Set<FlowDescription> flows = repo.getAvailableFlowDescriptions();
        Iterator<FlowDescription> iter = flows.iterator();
        FlowDescription flow = iter.next();
        String sResourceUrl = flow.getFlowComponentAsString();
        System.out.println(sResourceUrl);
        boolean retStat = false;
        Set<URI> flowsAfter = null;
        try{
            //Set<URL> flowsBefore = _meandreClient.retrieveFlowUrls();
            retStat = _meandreClient.uploadFlow(flow, true);
            assertTrue(retStat);
            //Set<URL> flowsMid = _meandreClient.retrieveFlowUrls();
            retStat =  _meandreClient.removeResource(sResourceUrl);
            assertTrue(retStat);
            flowsAfter = _meandreClient.retrieveFlowUris();
        }catch(Exception e){
            fail("TransmissionFailure: " + e.toString());
        }
        try{
            assertFalse("Contains resource URI", 
                    flowsAfter.contains(new URI(sResourceUrl)));
        }catch(Exception e){
            e.printStackTrace();
        }        

    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#publish(java.lang.String)}.
     */
    @Test
    public void testPublish() {
        String sUrl = "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        try{
            boolean ret = _meandreClient.publish(sUrl);
            assertTrue("!publish success", ret);
        }catch(Exception e){
            e.printStackTrace();
            fail(e.toString());
        }
        return;
        
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#unpublish(java.lang.String)}.
     */
    @Test
    public void testUnpublish() {
        String sUrl = "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        boolean ret;
        try{
            //ret = _meandreClient.publish(sUrl);
            //assertTrue("!publish success", ret);
            ret = _meandreClient.unpublish(sUrl);
            assertTrue("!unpublish success", ret);
        }catch(Exception e){
            e.printStackTrace();
            fail(e.toString());
        }
        return;       
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#runFlow(java.lang.String)}.
     */
    @Test
    public void testRunFlow() {
        String sUrl = 
                "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        String ret;
        try{
            
            ret = _meandreClient.runFlow(sUrl, false);
            System.out.println("testRunFlow flow output:");
            System.out.println(ret);
            
        }catch(Exception e){
            e.printStackTrace();
            fail(e.toString());
        }
        return;      
        
    }
    
    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#runFlow(java.lang.String, java.util.HashMap)}.
     */
    @Test
    public void testRunFlowWithProbes() {
        String sUrl = 
                "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        String ret;
        try{
            HashMap<String,String> hmap = new HashMap<String,String>();
            hmap.put("statistics", "true");
            ret = _meandreClient.runFlow(sUrl, hmap);
            System.out.println("testRunFlow flow output:");
            System.out.println(ret);
            
        }catch(Exception e){
            e.printStackTrace();
            fail(e.toString());
        }
        return;      
        
    }
    
    
    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#
     *      runFlowStreamOutput(java.lang.String)}.
     */
    @Test
    public void testRunFlowStreamOutput() {
        
        String sUrl = 
            "meandre://test.org/flow/test-hello-world-with-python-and-lisp/";
        
        try{
        
            InputStream ins = _meandreClient.runFlowStreamOutput(sUrl, false);
            System.out.println("testRunFlow flow output:");
            LineNumberReader reader = new LineNumberReader(
                    new InputStreamReader(ins));
            String str = "\n\n*******TEST::::BEGIN FLOW OUTPUT*******\n\n";
            while(str != null){
                System.out.println(str);
                str = reader.readLine();
            }
            System.out.println("\n\n*******TEST::::END FLOW OUTPUT*******\n\n");
        
        }catch(Exception e){
            e.printStackTrace();
            fail(e.toString());
        }
        return;       
                
    }
    /**
     * Test method for {@link 
     * org.meandre.client.v1.MeandreClient#retrieveRunningFlows()}.
     */
    @Test
    public void testRetrieveRunningFlows() {
        //fail("test not implemented");
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrievePublicRepository()}.
     */
    @Test
    public void testRetrievePublicRepository() {
        //QueryableRepository repo = null;
        try{
        	//repo = _meandreClient.retrievePublicRepository();
        	_meandreClient.retrievePublicRepository();
        }catch (TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        return;
    }

    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#retrieveDemoRepository()}.
     */
    @Test
    public void testRetrieveDemoRepository() {
        //QueryableRepository repo = null;
        try{
           //repo = _meandreClient.retrieveDemoRepository();
           _meandreClient.retrieveDemoRepository();
        }catch (TransmissionException e){
            fail("Transmission failure: " + e.toString());
        }
        return;
    }

    /**
     * Test method for 
     * {@link org.meandre.client.MeandreClient#uploadFiles(Set<File>, boolean)}.
     */
    @Test
    public void testRunRepository() {
        Model mod = DemoRepositoryGenerator.getTestHelloWorldMoreHetereogenousRepository();
        try{
            String ret = _meandreClient.runRepository(mod);
            System.out.println(ret);
            assertTrue(ret.length()>1000);
            assertTrue(ret.indexOf("Error")==-1);
        }catch(Exception e){
            e.printStackTrace();
            fail("could not complete upload");
        }
        return;
    }
    /**
     * Test method for {@link org.meandre.client.v1.MeandreClient#abortFlow()}.
     */
    @Test
    public void testAbortFlow() {
        //fail("test not implemented");
    }

    /**
     * Test method for 
     * {@link org.meandre.client.v1.MeandreClient#retrieveRunningFlowStatisitics()}.
     */
    @Test
    public void testRetrieveRunningFlowStatisitics() {
        //fail("test not implemented");
    }

}
