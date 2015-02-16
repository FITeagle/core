package org.fiteagle.core.reservation.dm;


import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import com.hp.hpl.jena.rdf.model.*;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.reservation.ReservationHandler;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_RESERVATION),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
public class ReservationMDBListener implements MessageListener {

    private static Logger LOGGER = Logger.getLogger(ReservationMDBListener.class.toString());

    @Inject
    private JMSContext context;
    @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    public void onMessage(final Message message) {
        String messageType = MessageUtil.getMessageType(message);
        String serialization = MessageUtil.getMessageSerialization(message);
        String rdfString = MessageUtil.getStringBody(message);
        LOGGER.log(Level.INFO, "Received a " + messageType + " message");

        if (messageType != null && rdfString != null) {
            Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);

            if (messageType.equals(IMessageBus.TYPE_CREATE)) {
                try {
                    handleCreate(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
                } catch (ResourceRepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (messageType.equals(IMessageBus.TYPE_GET)) {
                handleGet(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
            }
        }
    }

    private void handleGet(Model messageModel, String serialization, String jmsCorrelationID) {
        Message responseMessage = null;
        Model resultModel = ModelFactory.createDefaultModel();

        //getInfrastructure Slice URN or Sliver URNS

        ResIterator iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        if (iterator.hasNext()) {
            //should be only one resource
            Resource r = iterator.nextResource();
            String uri = r.getURI();
            String queryAssociatedReservations = "";
            try {
                queryAssociatedReservations = buildQueryForGroupReservations(uri);
                ResultSet rs = QueryExecuter.executeSparqlSelectQuery(queryAssociatedReservations);
                while (rs.hasNext()) {
                    QuerySolution qs = rs.next();
                    Resource result = qs.get("?reservationId").asResource();
                    Resource resource = resultModel.createResource(result.getURI(), Omn.Resource);
                    resource.addProperty(Omn_lifecycle.hasReservationState, qs.get("?state"));
                    resource.addProperty(MessageBusOntologyModel.endTime, qs.get("?endTime"));

                }
            } catch (ResourceRepositoryException e) {
                e.printStackTrace();
            }
            System.out.println(queryAssociatedReservations);
        } else {
            iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Resource);
            while (iterator.hasNext()) {
                Resource r = iterator.nextResource();
                System.out.println("ssss");
            }
        }
        String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, jmsCorrelationID, context);
        context.createProducer().send(topic, responseMessage);
    }

    private String buildQueryForGroupReservations(String uri) throws ResourceRepositoryException {
        String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> " +
                "SELECT ?reservationId ?state ?endTime WHERE {\n" +
                "?reservationId  omn:partOfGroup \"" + uri + "\".\n" +
                "?reservationId omn:hasState ?state .\n" +
                "?reservationId omn:endTime  ?endTime\n" +
                "}";
        return query;

    }

    private void handleCreate(Model requestModel, String serialization, String requestID) throws ResourceRepositoryException {
        LOGGER.log(Level.INFO, "handling reservation request ...");
        Message responseMessage = null;
        ReservationHandler reservationHandler = new ReservationHandler();
        Model reservationModel = reservationHandler.handleReservation(requestModel);


        String serializedResponse = MessageUtil.serializeModel(reservationModel, serialization);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);

        context.createProducer().send(topic, responseMessage);
    }




}
