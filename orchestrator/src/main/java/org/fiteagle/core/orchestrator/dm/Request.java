package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Resource;

import java.util.Map;

/**
 * Created by dne on 04.02.15.
 */

public  class Request {
    private final String requestId;
    private final Resource resource;
    private boolean handled;
    private final RequestContext context;

    public Request(String requestId, Resource resource, RequestContext context) {

        this.requestId = requestId;
        this.resource = resource;
        this.context =  context;
        handled = false;
        context.addRequest(this);
    }

    public void setHandled(){
        handled = true;
    }

    public String getRequestId(){
        return  requestId;
    }
    public Resource getResource(){
        return this.resource;
    }

    public RequestContext getContext() {
        return context;
    }

    public boolean isHandled(){
        return handled;
    }

}
