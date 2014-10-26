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
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ResourceRepoHandlerMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepoHandlerMDB implements MessageListener {

    private static Logger LOGGER = Logger.getLogger(ResourceRepoHandlerMDB.class.toString());
    
    private ResourceRepoHandler repository;
    
    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;
    
    @PostConstruct
    private void startUp(){
        repository = ResourceRepoHandler.getInstance();
    }

    public void onMessage(final Message message) {

        try {
            if (message.getStringProperty(IMessageBus.METHOD_TYPE) != null && message.getStringProperty(IMessageBus.RDF) != null) {
                String result = "";
                
                Model modelMessage = MessageBusMsgFactory.getMessageRDFModel(message);

                if (modelMessage != null) {
                    if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_INFORM)) {
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Received an INFORM message");
                        handleInform(modelMessage);

                    } else if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_REQUEST)) {
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Received a REQUEST message");
                        result = handleRequest(modelMessage, message.getStringProperty(IMessageBus.SERIALIZATION));

                    }
                }

                if (!result.isEmpty()) {
                    Message responseMessage = generateResponseMessage(message, result);

                    if (null != message.getJMSCorrelationID()) {
                        responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
                    }

                    this.context.createProducer().send(topic, responseMessage);
                }
            }

        } catch (JMSException e) {
          ResourceRepoHandlerMDB.LOGGER.log(Level.SEVERE, e.getMessage());
        }

    }
    
    public Message generateResponseMessage(Message requestMessage, String serializedRDF) throws JMSException {
        final Message responseMessage = this.context.createMessage();

        responseMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
        responseMessage.setStringProperty(IMessageBus.RDF, serializedRDF);
        responseMessage.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);

        return responseMessage;
    }
    
  private String handleRequest(Model modelRequest, String serialization) {
    Model response = null;
    
    StmtIterator iter = modelRequest.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest));
    Statement currentStatement = null;
    while (iter.hasNext()) {
      currentStatement = iter.nextStatement();
      
      if (currentStatement.getSubject().hasProperty(MessageBusOntologyModel.propertySparqlQuery)) {
        response = repository.handleSPARQLRequest(modelRequest, serialization);
      } else {
        modelRequest.remove(currentStatement);
        response = repository.handleRequest(modelRequest);
        MessageBusMsgFactory.setCommonPrefixes(response);
      }
      
      if (response != null) {
        return MessageBusMsgFactory.serializeModel(MessageBusMsgFactory.createMsgInform(response));
      }
    }
    
    return "";
  }

    private void handleInform(Model modelInform) {

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelInform.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform);
        Statement currentStatement = null;
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();
        }

        // This is an inform message, so do something with it
        if (currentStatement != null) {
            modelInform.remove(currentStatement);
            checkForReleases(modelInform);
            repository.addInformToRepository(modelInform);
        }
    }

    private void checkForReleases(Model modelInform) {
        Statement releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
        while(releaseStatement != null){
          repository.releaseResource(releaseStatement.getResource());
          LOGGER.log(Level.INFO, "Removing resource: " + releaseStatement.getResource());
          modelInform.remove(releaseStatement);
          releaseStatement = modelInform.getProperty(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodReleases);
        }
    }

}