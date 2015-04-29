package org.fiteagle.core.orchestrator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import info.openmultinet.ontology.vocabulary.Omn_service;

import org.fiteagle.core.orchestrator.dm.OrchestratorStateKeeper;
import org.fiteagle.core.orchestrator.dm.Request;
import org.fiteagle.core.orchestrator.dm.RequestContext;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Created by dne on 12.02.15.
 */
@Stateless
public class RequestHandler {
    @Inject
    OrchestratorStateKeeper stateKeeper;

    public void parseModel(RequestContext context, Model requestModel, String method) {

        Model requestedResources = getRequestedResources(requestModel);

        ResIterator resIterator = requestedResources.listSubjectsWithProperty(Omn_lifecycle.implementedBy);

        while (resIterator.hasNext()) {
            Resource requestedResource = resIterator.nextResource();
            String target = requestedResource.getProperty(Omn_lifecycle.implementedBy).getObject().asResource().getURI();
            Request request = context.getRequestByTarget(target);
            request.setMethod(method);
            request.addOrUpdate(requestedResource);
            stateKeeper.addRequest(request);

        }

    }

    private Model getRequestedResources(Model requestModel) {
        Model returnModel = ModelFactory.createDefaultModel();
        ResIterator resIterator = requestModel.listSubjectsWithProperty(RDF.type, Omn.Resource);
        if(!resIterator.hasNext()){
            ResIterator resIterator1 = requestModel.listSubjectsWithProperty(RDF.type,Omn.Topology);
            while (resIterator1.hasNext()){
                Resource topo = resIterator1.nextResource();
                requestModel.add(TripletStoreAccessor.getResource(topo.getURI()));
                resIterator = requestModel.listSubjectsWithProperty(Omn.isResourceOf, topo);
                while (resIterator.hasNext()){
                    Resource resource = resIterator.nextResource();
                    Statement keyStatement = topo.getProperty(Omn_service.publickey);
                    if(keyStatement!= null)
                        resource.addProperty(keyStatement.getPredicate(),keyStatement.getObject());

                    Statement username = topo.getProperty(Omn_service.username);
                    if(username != null)
                        resource.addProperty(username.getPredicate(),username.getObject());

                }
                resIterator = requestModel.listSubjectsWithProperty(Omn.isResourceOf, topo);
            }

        }

        while (resIterator.hasNext()) {
            Resource requestedResource = resIterator.nextResource();
            Model resourceModel = TripletStoreAccessor.getResource(requestedResource.getURI());

            if(requestModel.contains(requestedResource, Omn_service.publickey) && requestModel.contains(requestedResource, Omn_service.username)) {
              Statement publicKey = requestModel.getProperty(requestedResource, Omn_service.publickey);
              resourceModel.add(publicKey);
              Statement username = requestModel.getProperty(requestedResource, Omn_service.username);
              resourceModel.add(username);
            }
            
            
            returnModel.add(resourceModel);


        }
        return returnModel;

    }


    protected void setStateKeeper(OrchestratorStateKeeper stateKeeper) {
        this.stateKeeper = stateKeeper;
    }

}
