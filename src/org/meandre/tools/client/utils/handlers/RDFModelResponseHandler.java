package org.meandre.tools.client.utils.handlers;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Response handler that returns an RDF JENA Model
 * 
 * @author Boris Capitanu
 */
public class RDFModelResponseHandler implements ResponseHandler<Model> {

    private static RDFModelResponseHandler _instance;
    
    public static RDFModelResponseHandler getInstance() {
        if (_instance == null)
            _instance = new RDFModelResponseHandler();
        
        return _instance;
    }
    
    private RDFModelResponseHandler() { }
    
    public Model handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        
        return (entity != null) ? ModelUtils.getModel(entity.getContent(), null) : null;
    }
}
