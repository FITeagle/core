package org.fiteagle.core.repo.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
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
import org.fiteagle.core.repo.ResourceRepoHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
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
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  @PostConstruct
  private void startUp() {
    repoHandler = ResourceRepoHandler.getInstance();
  }
  
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
          handleRequest(messageModel, message.getStringProperty(IMessageBus.SERIALIZATION), message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleRequest(Model requestModel, String serialization, String requestID) throws JMSException {
    Resource messageResource = requestModel.getResource(MessageBusOntologyModel.internalMessage.getURI());
    
    if (messageResource.hasProperty(MessageBusOntologyModel.propertySparqlQuery)) {
      
      String serializedResponse = repoHandler.handleSPARQLRequest(requestModel, serialization);
      
      Message responseMessage = null;
      if(messageResource.hasProperty(MessageBusOntologyModel.methodRestores)){
        Model replyModel = MessageUtil.parseSerializedModel(serializedResponse);
        replyModel.add(messageResource.getProperty(MessageBusOntologyModel.methodRestores));
        serializedResponse = MessageUtil.serializeModel(replyModel);
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_CREATE, serialization, context);
      }
      else{
        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization, context);
        responseMessage.setJMSCorrelationID(requestID);
      }
      context.createProducer().send(topic, responseMessage);
    } else {
      LOGGER.log(Level.SEVERE, "No SPARQL query found");
    }
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