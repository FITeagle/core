package org.fiteagle.core.reservation;

import com.hp.hpl.jena.ontology.impl.ObjectPropertyImpl;
import com.hp.hpl.jena.rdf.model.*;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dne on 15.02.15.
 */
public class ReservationHandler {
    private static final Logger LOGGER = Logger.getLogger(ReservationHandler.class.getName());

    public Model handleReservation(Model requestModel) throws TripletStoreAccessor.ResourceRepositoryException {

        LOGGER.log(Level.INFO, "handle reservation ...");
        Model reservationModel = ModelFactory.createDefaultModel();
        if(checkType(requestModel, reservationModel)){
          createReservationModel(requestModel, reservationModel);
          reserve(reservationModel);
        }
        return reservationModel;
    }

    private Model createReservationModel(Model requestModel, Model reservationModel) {
        
        ResIterator iterator = requestModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        while (iterator.hasNext()) {
            Resource topology = iterator.nextResource();
            reservationModel.add(topology, RDF.type, Omn.Topology);
           if (TripletStoreAccessor.exists(topology.getURI())) {
               LOGGER.log(Level.INFO, "Topology already exists");
               Model topologyModel = TripletStoreAccessor.getResource(topology.getURI());
                reservationModel.add(topologyModel);

            }
        else {

               Resource newTopology = reservationModel.getResource(topology.getURI());
               Property property = reservationModel.createProperty(MessageBusOntologyModel.endTime.getNameSpace(), MessageBusOntologyModel.endTime.getLocalName());
               property.addProperty(RDF.type, OWL.FunctionalProperty);
               newTopology.addProperty(property, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(getDefaultExpirationTime()));
               if (topology.getProperty(Omn_lifecycle.hasAuthenticationInformation) != null)
                   newTopology.addProperty(Omn_lifecycle.hasAuthenticationInformation, topology.getProperty(Omn_lifecycle.hasAuthenticationInformation).getObject());


           }

//
//                   ResIterator resIterator = requestModel.listSubjectsWithProperty(RDF.type, Omn.Topology);
//
//                   while (resIterator.hasNext()) {
//                       Resource r = resIterator.nextResource();
//
//                       try {
//                           TripletStoreAccessor.addResource(r);
//                           Model m = ModelFactory.createDefaultModel();
//                           Resource resource = m.createResource(r.getURI());
//
//
//                       } catch (TripletStoreAccessor.ResourceRepositoryException e) {
//                           e.printStackTrace();
//                       }
//                   }
//
//
//           }
           
           
            ResIterator resIter =  requestModel.listResourcesWithProperty(Omn.isResourceOf, topology);
            while(resIter.hasNext()){
                Resource resource = resIter.nextResource();
                SimpleSelector selector= new SimpleSelector(resource, null,(Object) null);

                StmtIterator statementIter = requestModel.listStatements(selector);
                Resource newResource = reservationModel.createResource(resource.getNameSpace()+ UUID.randomUUID().toString());

                while(statementIter.hasNext()){
                    Statement statement = statementIter.nextStatement();
                    newResource.addProperty(statement.getPredicate(), statement.getObject());
                    if(statement.getPredicate().equals(Omn_lifecycle.usesService)){
                        StmtIterator serviceModel =requestModel.listStatements(new SimpleSelector(statement.getObject().asResource(),null,(Object) null));
                        reservationModel.add(serviceModel);
                    }
                }
                reservationModel.add(topology, Omn.hasResource,newResource);
            }



        }
        return reservationModel;
    }


    private Boolean checkType(Model requestModel, Model reservationModel){
      Boolean TYPE=true;
      ResIterator resIterator =  requestModel.listResourcesWithProperty(Omn.isResourceOf);
      Object type= null;
      Object adapterInstance = null;
      
      while(resIterator.hasNext()){
        
        Resource resource1 = resIterator.nextResource();
        
        if(resource1.hasProperty(RDF.type)){
          
          StmtIterator stmtIterator = resource1.listProperties(RDF.type);
          while(stmtIterator.hasNext()){
            
            Statement statement1 = stmtIterator.nextStatement();
            if(!Omn_resource.Node.equals(statement1.getObject())){
              type = statement1.getObject();
            }
          } 
        }
        if(resource1.hasProperty(Omn_lifecycle.implementedBy)){
          adapterInstance = resource1.getProperty(Omn_lifecycle.implementedBy).getObject();
        }
        
      
      
      Model mo = ModelFactory.createDefaultModel();
      Resource re = mo.createResource(adapterInstance.toString());
      Model model = TripletStoreAccessor.getResource(re.getURI());
      
      ResIterator resIter =  model.listResourcesWithProperty(Omn_lifecycle.canImplement);
      while(resIter.hasNext()){
        Resource res = resIter.nextResource();
        if(!type.equals(res.getProperty(Omn_lifecycle.canImplement).getObject())){
          TYPE = false;
          Resource resource = reservationModel.createResource(resource1.getURI());
          Property wrongType = resource.getModel().createProperty(Omn_resource.type.getNameSpace(), "hasWrongType");
          resource.addProperty(wrongType, type.toString());
          Property itsRightType = resource.getModel().createProperty(Omn_resource.type.getNameSpace(), "itsRightType");
          resource.addProperty(itsRightType, res.getProperty(Omn_lifecycle.canImplement).getObject().toString());
          LOGGER.log(Level.INFO, "ADAPTER INSTANCE CAN'T IMPLEMENT THIS TYPE");
        } 
      }
      }
      return TYPE;
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
