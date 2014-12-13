package org.fiteagle.core.repo.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.core.repo.ResourceRepoHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ResourceRepoHandlerMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepoHandlerMDB implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ResourceRepoHandlerMDB.class.toString());
  
  private ResourceRepoHandler repoHandler;
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  @PostConstruct
  private void startUp() {
    repoHandler = ResourceRepoHandler.getInstance();
  }
  
  public void onMessage(final Message message) {
    try {
      String messageType = message.getStringProperty(IMessageBus.METHOD_TYPE);
      Model messageModel = MessageBusMsgFactory.getMessageRDFModel(message);
      
      if (messageType != null && messageModel != null) {
        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, "Received a " + messageType + " message");
        
        if (messageType.equals(IMessageBus.TYPE_INFORM)) {
          handleInform(messageModel);
          
        } else if (messageType.equals(IMessageBus.TYPE_REQUEST)) {
          String result = handleRequest(messageModel, message.getStringProperty(IMessageBus.SERIALIZATION));
          if (result != null) {
            sendResponseMessage(message, result, message.getJMSCorrelationID());
          }
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void sendResponseMessage(Message requestMessage, String serializedRDF, String requestID) throws JMSException {
    final Message responseMessage = this.context.createMessage();
    
    responseMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
    responseMessage.setStringProperty(IMessageBus.RDF, serializedRDF);
    responseMessage.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);
    if (requestID != null) {
      responseMessage.setJMSCorrelationID(requestID);
    }
    
    this.context.createProducer().send(topic, responseMessage);
  }
  
  private String handleRequest(Model modelRequest, String serialization) {
    Model responseModel = null;
    
    StmtIterator iter = modelRequest.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
    while (iter.hasNext()) {
      Statement currentStatement = iter.nextStatement();
      
      if (currentStatement.getSubject().hasProperty(MessageBusOntologyModel.propertySparqlQuery)) {
        responseModel = repoHandler.handleSPARQLRequest(modelRequest, serialization);
      } else {
        modelRequest.remove(currentStatement);
        responseModel = repoHandler.handleRequest(modelRequest);
        MessageBusMsgFactory.setCommonPrefixes(responseModel);
      }
      
      if (responseModel != null) {
        return MessageBusMsgFactory.serializeModel(MessageBusMsgFactory.createMsgInform(responseModel));
      }
    }
    
    return null;
  }
  
  private void handleInform(Model modelInform) {
    StmtIterator iter = modelInform.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform);
    if (iter.hasNext()) {
      Statement currentStatement = iter.nextStatement();
      modelInform.remove(currentStatement);
      checkForReleases(modelInform);
      repoHandler.addInformToRepository(modelInform);
    }
  }
  
  private void checkForReleases(Model modelInform) {
    Statement releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
    while (releaseStatement != null) {
      repoHandler.releaseResource(releaseStatement.getResource());
      LOGGER.log(Level.INFO, "Removing resource: " + releaseStatement.getResource());
      modelInform.remove(releaseStatement);
      releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
    }
  }
  
}