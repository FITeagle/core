package org.fiteagle.core.repo.dm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

import org.apache.jena.riot.RiotException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.core.repo.ResourceRepoHandler;
import org.fiteagle.core.repo.OLDResourceRepository;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@MessageDriven(name = "ResourceInformListenerMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
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
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.toString() + " : Received an INFORM message");
                        handleInform(modelMessage);

                    } else if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_REQUEST)) {
                        ResourceRepoHandlerMDB.LOGGER.log(Level.INFO, this.toString() + " : Received a CREATE message");
                        result = handleRequest(modelMessage);

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
            System.err.println(this.toString() + "JMSException");
        }

    }
    
    public Message generateResponseMessage(Message requestMessage, String result) throws JMSException {
        final Message responseMessage = this.context.createMessage();

        responseMessage.setStringProperty(IMessageBus.TYPE_RESPONSE, IMessageBus.TYPE_INFORM);
        responseMessage.setStringProperty(IMessageBus.RDF, result);

        return responseMessage;
    }
    
    private String handleRequest(Model modelRequest) {

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelRequest.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest));
        Statement currentStatement = null;
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();
        }

        // This is an inform message, so do something with it
        if (currentStatement != null) {
            modelRequest.remove(currentStatement);
            Model response = repository.handleRequest(modelRequest);

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