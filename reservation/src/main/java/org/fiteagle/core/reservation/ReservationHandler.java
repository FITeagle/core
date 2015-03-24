package org.fiteagle.core.reservation;

import com.hp.hpl.jena.ontology.impl.ObjectPropertyImpl;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import org.fiteagle.api.core.IConfig;
import org.fiteagle.api.core.MessageBusOntologyModel;
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
        Model reservationModel =createReservationModel(requestModel);
        reserve(reservationModel);
        return reservationModel;
    }

    private Model createReservationModel(Model requestModel) {
        Model reservationModel = ModelFactory.createDefaultModel();
        ResIterator iterator = requestModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        while (iterator.hasNext()) {
            Resource topology = iterator.nextResource();
            reservationModel.add(topology, RDF.type, Omn.Topology);
           if (TripletStoreAccessor.exists(topology.getURI())) {
               LOGGER.log(Level.INFO, "Topology already exists");
               Model topologyModel = TripletStoreAccessor.getResource(topology.getURI());
                reservationModel.add(topologyModel);

            }
            ResIterator resIter =  requestModel.listResourcesWithProperty(Omn.isResourceOf, topology);
            while(resIter.hasNext()){
                Resource resource = resIter.nextResource();
                SimpleSelector selector= new SimpleSelector(resource, null,(Object) null);

                StmtIterator statementIter = requestModel.listStatements(selector);
                Resource newResource = reservationModel.createResource(resource.getNameSpace()+ UUID.randomUUID().toString());

                while(statementIter.hasNext()){
                    Statement statement = statementIter.nextStatement();
                    newResource.addProperty(statement.getPredicate(),statement.getObject());
                }
                reservationModel.add(topology, Omn.hasResource,newResource);
            }



        }
        return reservationModel;
    }


    public void reserve(Model model) {


        ResIterator resIterator = model.listResourcesWithProperty(Omn.isResourceOf);
        while(resIterator.hasNext()){

            Resource requestedResource = resIterator.nextResource();
            Resource reservation = model.createResource(IConfig.RESERVATION_NAMESPACE_VALUE+ UUID.randomUUID().toString());
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
