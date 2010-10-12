package org.meandre.client.utils.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.BufferedHttpEntity;

/**
 * Response handler that returns a stream
 * 
 * @author Boris Capitanu
 */
public class StreamResponseHandler implements ResponseHandler<InputStream> {

    private static StreamResponseHandler _instance;
    
    public static StreamResponseHandler getInstance() {
        if (_instance == null)
            _instance = new StreamResponseHandler();
        
        return _instance;
    }
    
    private StreamResponseHandler() { }
    
    public InputStream handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null)
            entity = new BufferedHttpEntity(entity);

        return (entity != null) ? entity.getContent() : null;
    }
}
