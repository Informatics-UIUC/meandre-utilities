package org.meandre.tools.client.utils.handlers;

import org.apache.http.impl.client.BasicResponseHandler;

/**
 * Response handler that returns a string
 * 
 * @author Boris Capitanu
 */
public class StringResponseHandler extends BasicResponseHandler {
    
    private static StringResponseHandler _instance;

    public static StringResponseHandler getInstance() {
        if (_instance == null)
            _instance = new StringResponseHandler();
        
        return _instance;
    }
    
    private StringResponseHandler() { }
}
