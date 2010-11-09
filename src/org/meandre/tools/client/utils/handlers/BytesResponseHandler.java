package org.meandre.tools.client.utils.handlers;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

/**
 * Response handler that returns a byte array
 * 
 * @author Boris Capitanu
 */
public class BytesResponseHandler implements ResponseHandler<byte[]> {
    
    private static BytesResponseHandler _instance;
   
    public static BytesResponseHandler getInstance() {
        if (_instance == null)
            _instance = new BytesResponseHandler();
        
        return _instance;
    }
    
    private BytesResponseHandler() { }
    
    public byte[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        
        return (entity != null) ? EntityUtils.toByteArray(entity) : null;
    }
}
