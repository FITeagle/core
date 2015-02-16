package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Resource;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
        if(!containsResource(resource)){
            resourceList.add(resource);
        }else{
            updateResource(resource);


        }

    }

    private void updateResource(Resource resource) {
        for(Iterator<Resource> iterator = resourceList.iterator();iterator.hasNext();){
            Resource resource1 = iterator.next();
           if(resource1.getURI().equals(resource.getURI())){
               resource1.getModel().add(resource.getModel());
           }


        }
    }

    private boolean containsResource(Resource resource) {
        boolean ret = false;
        for(Resource r: resourceList){
            if(r.getURI().equals(resource.getURI())){
                ret = true;
                break;
            }

        }
        return ret;
    }

    public List<Resource> getResourceList(){
        return this.resourceList;
    }
}
