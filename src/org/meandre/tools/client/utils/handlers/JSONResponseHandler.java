package org.meandre.tools.client.utils.handlers;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONTokener;

/**
 * Response handler that returns a JSON tokener
 * 
 * @author Boris Capitanu
 */
public class JSONResponseHandler implements ResponseHandler<JSONTokener> {

    private static JSONResponseHandler _instance;
    private final BasicResponseHandler _stringHandler = new BasicResponseHandler();
    
    public static JSONResponseHandler getInstance() {
        if (_instance == null)
            _instance = new JSONResponseHandler();
        
        return _instance;
    }
    
    private JSONResponseHandler() { }
    
    public JSONTokener handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        String result = _stringHandler.handleResponse(response);
        
        return (result != null && result.length() > 0) ? new JSONTokener(result) : null;
    }
}
