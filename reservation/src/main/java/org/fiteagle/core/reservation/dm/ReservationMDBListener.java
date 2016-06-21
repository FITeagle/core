package org.fiteagle.core.reservation.dm;


import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.fiteagle.core.reservation.ReservationHandler;



import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

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
        try {

        if (messageType != null && rdfString != null) {
            Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);

            if (messageType.equals(IMessageBus.TYPE_CREATE)) {

                    handleCreate(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));

            }
            if (messageType.equals(IMessageBus.TYPE_CONFIGURE)) {

                    handleConfigure(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));

            }
            if (messageType.equals(IMessageBus.TYPE_GET)) {
                handleGet(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
            }
        }        }
        catch (ResourceRepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidModelException e) {
            e.printStackTrace();
        }
    }

    private void handleConfigure(Model messageModel, String serialization, String jmsCorrelationID) throws InvalidModelException, ResourceRepositoryException {
        Message responseMessage = null;
        Model resultModel  = messageModel;
        ResIterator iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        if (iterator.hasNext()) {
            //should be only one resource
            TripletStoreAccessor.updateModel(messageModel);


        } else {
            iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Reservation);
            TripletStoreAccessor.updateModel(messageModel);
            while (iterator.hasNext()) {

                Resource reservation = iterator.nextResource();
                String uri = reservation.getURI();
                resultModel.add(TripletStoreAccessor.getResource(uri));
                addResourcesForReservations(resultModel);
                addWrappingTopology(resultModel);
            }
        }
        String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, jmsCorrelationID, context);
        context.createProducer().send(topic, responseMessage);
    }

    private void addResourcesForReservations(Model resultModel) {
        StmtIterator stmtIterator = resultModel.listStatements(new SimpleSelector(null, Omn.isReservationOf,(Object)null));
        while(stmtIterator.hasNext()){
            Resource resource = stmtIterator.nextStatement().getObject().asResource();
            resultModel.add(TripletStoreAccessor.getResource(resource.getURI()));
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
            resultModel = TripletStoreAccessor.getResource(uri);
            addResources(resultModel);
            addReservations(resultModel);
            addServices(resultModel);

        } else {
            iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Resource);
            while (iterator.hasNext()) {
                Resource r = iterator.nextResource();
                String uri = r.getURI();
                resultModel = TripletStoreAccessor.getResource(uri);
                addReservations(resultModel);
                addServices(resultModel);
                addWrappingTopology(resultModel);
            }
        }
        String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, jmsCorrelationID, context);
        context.createProducer().send(topic, responseMessage);
    }

    private void addServices(Model resultModel) {
        StmtIterator stmtIterator = resultModel.listStatements(new SimpleSelector(null, Omn.hasService,(Object)null));
        while(stmtIterator.hasNext()){
            Resource resource = stmtIterator.nextStatement().getObject().asResource();
            resultModel.add(TripletStoreAccessor.getResource(resource.getURI()));
        }
    }

    private void addWrappingTopology(Model resultModel) {
        StmtIterator stmtIterator = resultModel.listStatements(new SimpleSelector(null, Omn.hasResource,(Object)null));
        while(stmtIterator.hasNext()){
            Resource resource = stmtIterator.nextStatement().getSubject().asResource();
            resource.addProperty(RDF.type,Omn.Topology);
            resultModel.add(resource.listProperties());
            //resultModel.add(TripletStoreAccessor.getResource(resource.getURI()));
        }
    }

    private void addReservations(Model resultModel) {
        StmtIterator stmtIterator = resultModel.listStatements(new SimpleSelector(null, Omn.hasReservation,(Object)null));
        while(stmtIterator.hasNext()){
            Resource resource = stmtIterator.nextStatement().getObject().asResource();
            resultModel.add(TripletStoreAccessor.getResource(resource.getURI()));
        }
    }

    private void addResources(Model resultModel) {
        StmtIterator stmtIterator = resultModel.listStatements(new SimpleSelector(null, Omn.hasResource,(Object)null));
        CopyOnWriteArrayList<Statement> statementList = new CopyOnWriteArrayList<Statement>();
        for(Statement s : stmtIterator.toList()){
        	statementList.add(s);
        }
        Iterator<Statement> statementIterator = statementList.iterator();
        while(statementIterator.hasNext()){
            Resource resource = statementIterator.next().getObject().asResource();
            Model resourceModel = TripletStoreAccessor.getResource(resource.getURI());
            resultModel.add(resourceModel);
            
            Resource res = resourceModel.getResource(resource.getURI());
            StmtIterator stmtIter = res.listProperties();
            while(stmtIter.hasNext()){
              Statement statement = stmtIter.nextStatement();
              if(statement.getObject().isResource()){
                if(TripletStoreAccessor.exists(statement.getObject().asResource().getURI())){
                  if(!Omn_lifecycle.implementedBy.getLocalName().equals(statement.getPredicate().getLocalName())){
                    Model model = TripletStoreAccessor.getResource(statement.getObject().asResource().getURI());
                    resultModel.add(model);
                  }
                }
              }
            }
        }
    }


    private void handleCreate(Model requestModel, String serialization, String requestID) throws ResourceRepositoryException, ResourceRepositoryException {
        LOGGER.log(Level.INFO, "handling reservation request ...");
        ReservationHandler reservationHandler = new ReservationHandler();
        Message responseMessage = reservationHandler.handleReservation(requestModel, serialization, requestID, context);

        context.createProducer().send(topic, responseMessage);
    }




}
