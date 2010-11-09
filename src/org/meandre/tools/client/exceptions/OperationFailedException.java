package org.meandre.tools.client.exceptions;

import org.json.JSONObject;

/**
 * 
 * @author Boris Capitanu
 *
 */
public class OperationFailedException extends Exception {

    private static final long serialVersionUID = 5874125928264556261L;
    private final JSONObject _failure;

    public OperationFailedException(String message) {
        super(message);
        _failure = null;
    }
    
    public OperationFailedException(String message, JSONObject failure) {
        super(message);
        _failure = failure;
    }
    
    public JSONObject getFailure() {
        return _failure;
    }
}
