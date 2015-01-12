package org.fiteagle.core.orchestrator.dm;

import java.util.HashMap;
import java.util.Map;
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
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.orchestrator.provision.HandleProvision;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import javax.ws.rs.core.Response;

@MessageDriven(name = "OrchestratorMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_ORCHESTRATOR),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrchestratorMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(OrchestratorMDBListener.class.toString());
  
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
      if (messageType.equals(IMessageBus.TYPE_CONFIGURE)) {
        handleConfigureRequest(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
      }
      if (messageType.equals(IMessageBus.TYPE_DELETE)) {
        handleDeleteRequest(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
      }
    }
  }
  
  private void handleConfigureRequest(Model requestModel, String serialization, String requestID) {
    LOGGER.log(Level.INFO, "handling provision request ...");
    Message responseMessage = null;
    Model resultModel = ModelFactory.createDefaultModel();
    
    StmtIterator iterator = requestModel.listStatements(null, RDF.type, MessageBusOntologyModel.classGroup);
    while (iterator.hasNext()) {
      Model createModel = ModelFactory.createDefaultModel();
      Resource slice = iterator.next().getSubject();
      LOGGER.log(Level.INFO, "trying to provision this URN " + slice.getURI());
      Map<String, Object> reservations = null;
      try {
        reservations = HandleProvision.getReservations(slice.getURI());
        createModel = HandleProvision.createRequest(reservations);
        LOGGER.log(Level.INFO, createModel.getGraph().toString());
        String serializedModel = MessageUtil.serializeModel(createModel, IMessageBus.SERIALIZATION_TURTLE);
        LOGGER.log(Level.INFO, "message contains " + serializedModel);
        Model receivedModel = sendMessage(serializedModel, IMessageBus.TYPE_CREATE, IMessageBus.TARGET_ADAPTER);
        HandleProvision.reservationToRemove(receivedModel, reservations);
        TripletStoreAccessor.updateRepositoryModel(receivedModel);
        LOGGER.log(Level.INFO, "Reservation is deleted from Database");
      } catch (ResourceRepositoryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    /*
     * String serializedResponse = MessageUtil.serializeModel(resultModel, serialization); responseMessage =
     * MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID,
     * context); LOGGER.log(Level.INFO, " a reply is sent to SFA ..."); context.createProducer().send(topic,
     * responseMessage);
     */
  }
  
  private void handleDeleteRequest(Model requestModel, String serialization, String requestID) {
    LOGGER.log(Level.INFO, "handling provision request ...");
    // Message responseMessage = null;
    // Model resultModel = ModelFactory.createDefaultModel();
    
    // String serializedResponse = MessageUtil.serializeModel(resultModel);
    // responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization,
    // requestID, context);
    // LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    // context.createProducer().send(topic, responseMessage);
  }
  
  private Model sendMessage(String model, String methodType, String methodTarget) {
    final Message request = MessageUtil.createRDFMessage(model, methodType, methodTarget,
        IMessageBus.SERIALIZATION_TURTLE, null, context);
    context.createProducer().send(topic, request);
    LOGGER.log(Level.INFO, methodType + " message is sent to resource adapter");
    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(rcvMessage);
    
    if (MessageUtil.getMessageType(rcvMessage).equals(IMessageBus.TYPE_ERROR)) {
      if (resultString.equals(Response.Status.REQUEST_TIMEOUT.name())) {
        throw new MessageUtil.TimeoutException("Sent message (" + methodType + ") (Target: " + methodTarget + "): "
            + MessageUtil.getStringBody(request));
      }
      throw new RuntimeException(resultString);
    } else {
      LOGGER.log(Level.INFO, "Orchestratro received a reply");
      return MessageUtil.parseSerializedModel(resultString, IMessageBus.SERIALIZATION_TURTLE);
    }
  }
  
}
