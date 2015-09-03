package org.fiteagle.core.orchestrator;

import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_component;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import info.openmultinet.ontology.vocabulary.Omn_service;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.core.orchestrator.dm.OrchestratorStateKeeper;
import org.fiteagle.core.orchestrator.dm.Request;
import org.fiteagle.core.orchestrator.dm.RequestContext;
//import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
//import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

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

        Model requestedResources = this.getRequestedResources(requestModel, method);
          
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

    private Model getRequestedResources(Model requestModel, String method) {
        Model returnModel = ModelFactory.createDefaultModel();
        ResIterator resIterator = requestModel.listSubjectsWithProperty(RDF.type, Omn.Resource);
        if(!resIterator.hasNext()){
            ResIterator resIterator1 = requestModel.listSubjectsWithProperty(RDF.type,Omn.Topology);
            while (resIterator1.hasNext()){
                Resource topo = resIterator1.nextResource();
                
                Model topoModel = TripletStoreAccessor.getResource(topo.getURI());
                
                requestModel.add(topoModel);
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
            
            // provision only resources with reservationState "Allocated"
            if(resourceModel.contains(requestedResource, Omn.hasReservation)){
              Resource reservation = resourceModel.getProperty(requestedResource, Omn.hasReservation).getObject().asResource();
              Model reservationModel = TripletStoreAccessor.getResource(reservation.getURI());
              String reservationState = reservationModel.getProperty(reservation, Omn_lifecycle.hasReservationState).getObject().asResource().getURI();
              
              switch(method){
                case IMessageBus.TYPE_CREATE:
                  
                  if(reservationState.equals(Omn_lifecycle.Allocated.getURI())){
                    returnModel.add(resourceModel);
                  }
                  break;
                  
                case IMessageBus.TYPE_DELETE:
                  
                  if(reservationState.equals(Omn_lifecycle.Provisioned.getURI()) || reservationState.equals(Omn_lifecycle.Allocated.getURI())){
                    returnModel.add(resourceModel);
                  }
                  break;
                  
                  default:
                    returnModel.add(resourceModel);
                  
              }
              
            }

        }
        return returnModel;

    }

    public String isValidURN(Model requestModel){
      String error_message = "";
      ResIterator resIterator = requestModel.listSubjectsWithProperty(RDF.type, Omn.Resource);
      if(!resIterator.hasNext()){
        ResIterator resIterator1 = requestModel.listSubjectsWithProperty(RDF.type,Omn.Topology);
        error_message = checkURN(resIterator1, error_message);
      }
      error_message = checkURN(resIterator, error_message);
      return error_message;
    }
    
    private String checkURN(ResIterator resIterator, String error_message){
      while (resIterator.hasNext()){
        Resource resource = resIterator.nextResource();
        Model model = TripletStoreAccessor.getResource(resource.getURI());
        if(model.isEmpty() || model == null){
          error_message += resource.getURI() + " is not a valid urn. Please execute first allocate successfully.";
          }
        }
      return error_message;
      }

    protected void setStateKeeper(OrchestratorStateKeeper stateKeeper) {
        this.stateKeeper = stateKeeper;
    }

}
