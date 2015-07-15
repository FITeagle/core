package org.fiteagle.core.reservation;

import com.hp.hpl.jena.ontology.impl.ObjectPropertyImpl;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_component;
import info.openmultinet.ontology.vocabulary.Omn_domain_pc;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import info.openmultinet.ontology.vocabulary.Omn_resource;

import org.fiteagle.api.core.*;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSContext;
import javax.jms.Message;

/**
 * Created by dne on 15.02.15.
 */
public class ReservationHandler {
    private static final Logger LOGGER = Logger.getLogger(ReservationHandler.class.getName());

    public Message handleReservation(Model requestModel, String serialization, String requestID, JMSContext context) throws TripletStoreAccessor.ResourceRepositoryException {

        LOGGER.log(Level.INFO, "handle reservation ...");
        Message responseMessage = null;
        String errorMessage = checkType(requestModel);
        if(errorMessage == null || errorMessage.isEmpty()){
          Model reservationModel = ModelFactory.createDefaultModel();
          createReservationModel(requestModel, reservationModel);
          reserve(reservationModel);
          String serializedResponse = MessageUtil.serializeModel(reservationModel, serialization);
          responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
        }
        else {
          responseMessage = MessageUtil.createErrorMessage(errorMessage, requestID, context);
        }
        return responseMessage;
    }

    private Model createReservationModel(Model requestModel, Model reservationModel) {
        
      Map<String, Resource> resourcesIDs = new HashMap<String, Resource>();
      Model assistantModel = ModelFactory.createDefaultModel();
      
        ResIterator iterator = requestModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        while (iterator.hasNext()) {
            Resource topology = iterator.nextResource();
            assistantModel.add(topology, RDF.type, Omn.Topology);
           if (TripletStoreAccessor.exists(topology.getURI())) {
               LOGGER.log(Level.INFO, "Topology already exists");
               Model topologyModel = TripletStoreAccessor.getResource(topology.getURI());
               
               ResIterator iter = topologyModel.listResourcesWithProperty(RDF.type, Omn.Topology);
               while(iter.hasNext()){
                 Resource topo = iter.nextResource();
                 if(topo.hasProperty(MessageBusOntologyModel.endTime)){
                   Statement endTimeStmt = topo.getProperty(MessageBusOntologyModel.endTime);
                   assistantModel.add(topo, endTimeStmt.getPredicate(), endTimeStmt.getString());
                 }
                 
                 if(topo.hasProperty(Omn_lifecycle.hasAuthenticationInformation)){
                   Statement hasAuthenticationInformationStmt = topo.getProperty(Omn_lifecycle.hasAuthenticationInformation);
                   assistantModel.add(topo, hasAuthenticationInformationStmt.getPredicate(), hasAuthenticationInformationStmt.getObject());
                 }
               }

            }
        else {

               Resource newTopology = assistantModel.getResource(topology.getURI());
               Property property = assistantModel.createProperty(MessageBusOntologyModel.endTime.getNameSpace(), MessageBusOntologyModel.endTime.getLocalName());
               property.addProperty(RDF.type, OWL.FunctionalProperty);
               newTopology.addProperty(property, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(getDefaultExpirationTime()));
               if (topology.getProperty(Omn_lifecycle.hasAuthenticationInformation) != null)
                   newTopology.addProperty(Omn_lifecycle.hasAuthenticationInformation, topology.getProperty(Omn_lifecycle.hasAuthenticationInformation).getObject());


           }

//            ResIterator resIter =  requestModel.listResourcesWithProperty(Omn.isResourceOf, topology);
            ResIterator resIter = requestModel.listSubjects();
            
            
            
            while(resIter.hasNext()){
                Resource resource = resIter.nextResource();
                
                if(resource.hasProperty(Omn.isResourceOf)){
                
                SimpleSelector selector= new SimpleSelector(resource, null,(Object) null);

                StmtIterator statementIter = requestModel.listStatements(selector);
                Resource newResource = assistantModel.createResource(resource.getNameSpace()+ UUID.randomUUID().toString());

               resourcesIDs.put(resource.getURI(), newResource);
                
                while(statementIter.hasNext()){
                    Statement statement = statementIter.nextStatement();

                    newResource.addProperty(statement.getPredicate(), statement.getObject());
                    
                    
                    if(statement.getPredicate().equals(Omn_lifecycle.usesService)){
                        StmtIterator serviceModel =requestModel.listStatements(new SimpleSelector(statement.getObject().asResource(),null,(Object) null));
                        assistantModel.add(serviceModel);
                    }
               
                }
                assistantModel.add(topology, Omn.hasResource,newResource);
                
            } 
                else if(resource.hasProperty(Omn.hasResource)){
              
            } else {
                
//                if(resource.hasProperty(Omn.isResourceOf) || !resource.hasProperty(Omn.hasResource)){
                  StmtIterator stmtIterator = resource.listProperties();
                  while(stmtIterator.hasNext()){
                    Statement statement = stmtIterator.nextStatement();
                    assistantModel.add(statement);
                  }

                }
                
                
                
            }



        }

        
        ResIterator resIter = assistantModel.listSubjects();
        while(resIter.hasNext()){
          Resource res = resIter.nextResource();
          StmtIterator stmtIter = res.listProperties();
          while(stmtIter.hasNext()){
            Statement stmt = stmtIter.nextStatement();
            if("deployedOn".equals(stmt.getPredicate().getLocalName()) || "requires".equals(stmt.getPredicate().getLocalName())){
              Statement newStatement = new StatementImpl(stmt.getSubject(), stmt.getPredicate(), resourcesIDs.get(stmt.getObject().toString()));
              reservationModel.add(newStatement);
            }
            else{
              reservationModel.add(stmt);
            }
          }
        }
        
        

        
        return reservationModel;
    }


  private String checkType(Model requestModel) {
    String errorMessage = "";
    ResIterator resIterator = requestModel.listResourcesWithProperty(Omn.isResourceOf);
    RDFNode type = null;
    Object adapterInstance = null;
    
    while (resIterator.hasNext()) {
      
      Resource resource1 = resIterator.nextResource();
      
      if (resource1.hasProperty(RDF.type)) {
        
        StmtIterator stmtIterator = resource1.listProperties(RDF.type);
        while (stmtIterator.hasNext()) {
          
          Statement statement1 = stmtIterator.nextStatement();
          if (!Omn_resource.Node.equals(statement1.getObject())) {
            type = statement1.getObject();
          }
        }
      }
      if (resource1.hasProperty(Omn_lifecycle.implementedBy)) {
        adapterInstance = resource1.getProperty(Omn_lifecycle.implementedBy).getObject();
      }
      
      Model mo = ModelFactory.createDefaultModel();
      Resource re = mo.createResource(adapterInstance.toString());
      Model model = TripletStoreAccessor.getResource(re.getURI());
      if (model.isEmpty() || model == null) {
        errorMessage += "The requested component id " + re.getURI() + " is not supported";
      } else 
        if(!model.contains(re, Omn_lifecycle.canImplement, type)){
          errorMessage = "The requested sliver type " + type.toString()
              + " is not supported. Please see supported sliver types";
        }
      
    }
    return errorMessage;
  }
    
    public void reserve(Model model) {


        ResIterator resIterator = model.listResourcesWithProperty(Omn.isResourceOf);
        while(resIterator.hasNext()){

            Resource requestedResource = resIterator.nextResource();
            Config config =new Config();
            Resource reservation = model.createResource(config.getProperty(IConfig.LOCAL_NAMESPACE).concat("reservation/")+ UUID.randomUUID().toString());
            reservation.addProperty(RDFS.label, reservation.getURI());
            reservation.addProperty(RDF.type,Omn.Reservation);
            requestedResource.addProperty(Omn.hasReservation, reservation);
            reservation.addProperty(Omn.isReservationOf, requestedResource);
            Date afterAdding2h = getDefaultExpirationTime();
            reservation.addProperty(MessageBusOntologyModel.endTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(afterAdding2h));
            reservation.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Allocated);
            Property property  = model.createProperty(Omn_lifecycle.hasState.getNameSpace(), Omn_lifecycle.hasState.getLocalName());
            property.addProperty(RDF.type, OWL.FunctionalProperty);
            requestedResource.addProperty(property,Omn_lifecycle.Uncompleted);


        }


        try {
            TripletStoreAccessor.addModel(model);


        } catch (TripletStoreAccessor.ResourceRepositoryException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        } catch (InvalidModelException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }


    private static Date getDefaultExpirationTime() {
        Date date = new Date();
        long t = date.getTime();
        return new Date(t + (120 * 60000));
    }

}
