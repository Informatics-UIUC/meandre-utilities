package org.meandre.client.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.meandre.client.exceptions.TransmissionException;
import org.seasr.meandre.support.generic.util.KeyValuePair;

/**
 * 
 * @author Boris Capitanu
 *
 */
public class GenericHttpClient {
    
    private final HttpHost _host;
    private final DefaultHttpClient _httpClient;
    private Logger _logger;
    
    
    public GenericHttpClient(String host, int port) {
       this(host, port, null);
    }
    
    public GenericHttpClient(HttpHost host) {
        this(host, null);
    }
    
    public GenericHttpClient(String host, int port, Logger logger) {
        this(new HttpHost(host, port), logger);
    }
    
    public GenericHttpClient(HttpHost host, Logger logger) {
        _host = host;
        _logger = logger;

        HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

                // If no auth scheme has been initialized yet
                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                    // Obtain credentials matching the target host
                    Credentials creds = credsProvider.getCredentials(authScope);
                    // If found, generate BasicScheme preemptively
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        };

        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 200);
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
        SchemeRegistry schreg = new SchemeRegistry();
        schreg.register(new Scheme(_host.getSchemeName(), PlainSocketFactory.getSocketFactory(), _host.getPort()));
        
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schreg);
        _httpClient = new DefaultHttpClient(cm, params);
        // Add as the very first interceptor in the protocol chain
        _httpClient.addRequestInterceptor(preemptiveAuth, 0);
    }

    public Logger getLogger() {
        return _logger;
    }
    
    public void setLogger(Logger logger) {
        _logger = logger;
    }
    
    public HttpHost getHost() {
        return _host;
    }
    
    public void setCredentials(String userName, String password) {
        UsernamePasswordCredentials credentials = null;
        
        if (userName != null && password != null)
            credentials = new UsernamePasswordCredentials(userName, password);
        
        setCredentials(credentials);
    }
    
    public void setCredentials(Credentials credentials) {
        CredentialsProvider credentialsProvider = _httpClient.getCredentialsProvider();
        
        if (credentials != null)
            credentialsProvider.setCredentials(
                    new AuthScope(_host.getHostName(), _host.getPort()), credentials);
        else
            credentialsProvider.clear();
    }

    public <T> T doGET(String reqPath, List<Header> headers, ResponseHandler<T> handler, NameValuePair... params) throws TransmissionException {
        if (params.length > 0)
            reqPath += "?" + URLEncodedUtils.format(Arrays.asList(params), "UTF-8");
 
        HttpGet httpGet = new HttpGet(reqPath);     
        if (headers != null)
            for (Header header : headers)
                httpGet.addHeader(header);
        
        try {
            return _httpClient.execute(_host, httpGet, handler);
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }
    
    public InputStream doGET(String reqPath, List<Header> headers, NameValuePair... params) throws TransmissionException {
        if (params.length > 0)
            reqPath += "?" + URLEncodedUtils.format(Arrays.asList(params), "UTF-8");
 
        HttpGet httpGet = new HttpGet(reqPath);     
        if (headers != null)
            for (Header header : headers)
                httpGet.addHeader(header);
        
        try {
            HttpResponse response = _httpClient.execute(_host, httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            
            return new BufferedInputStream(stream);
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }
    
    public <T> T doPOST(String reqPath, List<Header> headers, List<KeyValuePair<String, ContentBody>> parts, 
                        ResponseHandler<T> handler, NameValuePair... params) throws TransmissionException, UnsupportedEncodingException {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        for (NameValuePair param : params)
            entity.addPart(param.getName(), new StringBody(param.getValue()));

        for (KeyValuePair<String, ContentBody> part : parts)
            entity.addPart(part.getKey(), part.getValue());

        HttpPost httpPost = new HttpPost(reqPath);
        if (headers != null)
            for (Header header : headers)
                httpPost.addHeader(header);
        
        httpPost.setEntity(entity);

        try {    
            return _httpClient.execute(_host, httpPost, handler);
        }
        catch (Exception e) {
            throw new TransmissionException(e);
        }
    }
    
    public void close() {
        _httpClient.getConnectionManager().shutdown();
    }
}
