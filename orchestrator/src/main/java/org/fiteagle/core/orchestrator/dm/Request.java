package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

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
    private String method;

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

    public void addOrUpdate(Resource resource) {
        //TODO check type and only add Resources
        if(resource.hasProperty(Omn.isResourceOf)) {
            if (!containsResource(resource)) {
                resourceList.add(resource);
            } else {

                updateResource(resource);


            }
        }

    }

    private void updateResource(Resource resource) {
        //TODO update functional properties if given
        for(Iterator<Resource> iterator = resourceList.iterator();iterator.hasNext();){
            Resource resource1 = iterator.next();
           if(resource1.getURI().equals(resource.getURI())){
               StmtIterator stmtIterator = resource.listProperties();
               while(stmtIterator.hasNext()){
                   Statement statement = stmtIterator.next();
                   Property property = statement.getPredicate();
                   if(property.hasProperty(RDF.type)){
                       if(property.getProperty(RDF.type).getObject().equals(OWL.FunctionalProperty)){
                           if(resource1.hasProperty(property))
                                resource1.removeAll(property);
                       }
                   }
                  resource1.getModel().add(statement);
               }
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
