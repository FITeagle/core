package org.fiteagle.core.repo.dm;

import java.util.LinkedList;
import java.util.List;
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
        // @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepoHandlerMDB implements MessageListener {

    private static Logger LOGGER = Logger.getLogger(ResourceRepoHandlerMDB.class.toString());
    
    private ResourceRepoHandler repository;
    
    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;
    
    @PostConstruct
    @SuppressWarnings("unused")
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
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.toString() + " : Received an INFORM message " + message.getJMSCorrelationID());
                        handleInform(modelMessage);

                    } else if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_REQUEST)) {
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.toString() + " : Received a REQUEST message" + message.getJMSCorrelationID());
                        result = handleRequest(modelMessage);

                    }
                }

                if (!result.isEmpty()) {
                    Message responseMessage = generateResponseMessage(message, result);

                    if (null != message.getJMSCorrelationID()) {
                        responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
                    }
                    //responseMessage.setJMSCorrelationID(UUID.randomUUID().toString());
                    

                    this.context.createProducer().send(topic, responseMessage);
                }
            }

        } catch (JMSException e) {
            System.err.println(this.toString() + "JMSException");
        }

    }
    
    public Message generateResponseMessage(Message requestMessage, String serializedRDF) throws JMSException {
        final Message responseMessage = this.context.createMessage();

        responseMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
        responseMessage.setStringProperty(IMessageBus.RDF, serializedRDF);
        responseMessage.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);

        return responseMessage;
    }
    
    private String handleRequest(Model modelRequest) {

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelRequest.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest));
        Statement currentStatement = null;
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();
        }

        // This is a request message, so do something with it
        if (currentStatement != null) {
            modelRequest.remove(currentStatement);
            Model response = repository.handleRequest(modelRequest);
            
            MessageBusMsgFactory.setCommonPrefixes(response);

            return MessageBusMsgFactory.serializeModel(MessageBusMsgFactory.createMsgInform(response));
        }
        return "";
    }

    private void handleInform(Model modelInform) {

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelInform.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform));
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
        List<Statement> releasesStatementsToRemove = new LinkedList<Statement>();

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelInform.listStatements();
        Statement currentStatement = null;
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();

            if (currentStatement.getPredicate().equals(MessageBusOntologyModel.methodReleases)) {
                repository.releaseResource(currentStatement.getResource());
                System.err.println("RDF Repo: Removing resource: " + currentStatement.getResource());
                releasesStatementsToRemove.add(currentStatement);

            }
        }

        for (Statement statement : releasesStatementsToRemove) {
            modelInform.remove(statement);
        }

    }

}
