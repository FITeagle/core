package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Resource;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dne on 04.02.15.
 */
public class RequestContext {
    public RequestContext(String requestContextId) {
        this.requestContextId = requestContextId;
        this.requestMap = new HashMap<>();
    }

    private String requestContextId;
    private Map<String,Request> requestMap;

    public Map<String, Request> getRequestMap() {
        return requestMap;
    }



    public String getRequestContextId() {
        return requestContextId;
    }



    public Request getRequest(String requestId){
        return requestMap.get(requestId);
    }

    public void addRequest(Request request){
        requestMap.put(request.getRequestId(),request);
    }


    public boolean allAnswersReceived(){
        boolean returnValue = true;
        for(String s: requestMap.keySet()){
            if(!requestMap.get(s).isHandled()) {
                returnValue = false;
                break;
            }
        }

        return returnValue;
    }

    public Request getRequestByTarget(String resource) {

        Request request = null;
        for( Request r: this.requestMap.values()){
            if(r.getTarget().equals(resource)){
                request =r;
               break;
            }
        }
        if(request == null){
            request = new Request(UUID.randomUUID().toString(),resource,this);
        }
        return  request;
    }
}
