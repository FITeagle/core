package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by dne on 04.02.15.
 */

public  class Request {
    private final String requestId;
    private final String target;
    private boolean handled;
    private final RequestContext context;
    private List<Resource> resourceList;

    public Request(String requestId, String target, RequestContext context) {

        this.requestId = requestId;
        this.target = target;
        this.context =  context;
        handled = false;
        context.addRequest(this);
        this.resourceList = new LinkedList<>();
    }

    public void setHandled(){
        handled = true;
    }

    public String getRequestId(){
        return  requestId;
    }


    public RequestContext getContext() {
        return context;
    }

    public boolean isHandled(){
        return handled;
    }

    public String getTarget() {
        return  this.target;
    }

    public void addResource(Resource resource) {
        resourceList.add(resource);
    }

    public List<Resource> getResourceList(){
        return this.resourceList;
    }
}
