package org.fiteagle.core.repository.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

@MessageDriven(name = "ResourceRepoMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepoMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ResourceRepoMDBListener.class.toString());
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  public void onMessage(final Message message) {
    try {
      String messageType = message.getStringProperty(IMessageBus.METHOD_TYPE);
      String serialization = message.getStringProperty(IMessageBus.SERIALIZATION);
      String rdfString = MessageUtil.getRDFResult(message);
      
      if (messageType != null && rdfString != null) {
        Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
        
        if (messageType.equals(IMessageBus.TYPE_INFORM)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleInform(messageModel);
          
        } else if (messageType.equals(IMessageBus.TYPE_REQUEST)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleRequest(messageModel, serialization, message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleRequest(Model requestModel, String serialization, String requestID) throws JMSException {
    Message responseMessage = null;
    try {
      String serializedResponse = TripletStoreAccessor.handleSPARQLRequest(requestModel, serialization);
      Resource messageResource = requestModel.getResource(MessageBusOntologyModel.internalMessage.getURI());
      if (messageResource.hasProperty(MessageBusOntologyModel.methodRestores)) {
        Model replyModel = MessageUtil.parseSerializedModel(serializedResponse);
        replyModel.add(messageResource.getProperty(MessageBusOntologyModel.methodRestores));
        serializedResponse = MessageUtil.serializeModel(replyModel);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_CREATE, serialization, null, context);
      } else {
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization, requestID, context);
      }
    } catch (ResourceRepositoryException | ParsingException e) {
      responseMessage = MessageUtil.createErrorMessage(e.getMessage(), requestID, context);
    } finally {
      context.createProducer().send(topic, responseMessage);
    }
  }
  
  private void handleInform(Model modelInform) {
    checkForReleases(modelInform);
    try {
      TripletStoreAccessor.updateRepositoryModel(modelInform);
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void checkForReleases(Model modelInform) {
    Statement releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
    while (releaseStatement != null) {
      try {
        TripletStoreAccessor.releaseResource(releaseStatement.getResource());
      } catch (ResourceRepositoryException e) {
        LOGGER.log(Level.SEVERE, e.getMessage());
      }
      LOGGER.log(Level.INFO, "Removing resource: " + releaseStatement.getResource());
      modelInform.remove(releaseStatement);
      releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
    }
  }
}