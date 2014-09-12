package org.fiteagle.core.repo.dm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.vocabulary.XSD;
import org.apache.jena.riot.RiotException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.core.repo.ResourceRequestListener;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@MessageDriven(name = "ResourceRequestListenerMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
        // @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRequestListenerMDB implements MessageListener {

    Logger LOGGER = Logger.getLogger(ResourceRequestListenerMDB.class.toString());
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
	/**
	 * PLEASE MERGE ME WITH ResourceInformListenerMDB
	 */

    public void onMessage(final Message message) {

        try {
            if (message.getStringProperty(IMessageBus.METHOD_TYPE) != null && message.getStringProperty(IMessageBus.RDF) != null) {

                if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_REQUEST)) {

                    Model resultModel = ModelFactory.createDefaultModel();
                    String inputRDF = message.getStringProperty(IMessageBus.RDF);


                    // create an empty model
                    Model modelRequest = ModelFactory.createDefaultModel();

                    InputStream is = new ByteArrayInputStream(inputRDF.getBytes());

                    try {

                        // read the RDF/XML file
                        modelRequest.read(is, null, message.getStringProperty(IMessageBus.SERIALIZATION));

                        String sparqlQuery = getQueryFromModel(modelRequest);

                        // This is a request message, so query the database with the given sparql query
                        if (!sparqlQuery.isEmpty()) {
                            ResultSet resultSet = ResourceRequestListener.queryModelFromDatabase(sparqlQuery);
                            String jsonString = getResultSetAsJsonString(resultSet);


                            //create result containing model
                            Model returnModel = MessageBusMsgFactory.createMsgInform(resultModel);
                            com.hp.hpl.jena.rdf.model.Resource r = returnModel.getResource("http://fiteagleinternal#Message");
                            r.addProperty(RDFS.comment, jsonString);

                            String serializedRDF = MessageBusMsgFactory.serializeModel(returnModel);



                            LOGGER.log(Level.INFO, "JSONString ResultSet before sending over Message Bus" +jsonString);

                            // generate reply message
                            final Message replyMessage = this.context.createMessage();
                            replyMessage.setJMSCorrelationID(message.getJMSCorrelationID());
                            replyMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
                            replyMessage.setStringProperty(IMessageBus.RDF, serializedRDF);                                       
                            
                            this.context.createProducer().send(topic, replyMessage);
                            
                        } else {
                        	// NOTHING FOUND
                        	System.err.println("No sparql query found!");
                        }
                    } catch (RiotException e) {
                        System.err.println("Invalid RDF");
                    }
                    
                }
            }

        } catch (JMSException e) {
            System.err.println("JMSException");
        }

    }

    /**
     * Get the comment section of the rdf model
     * @param model
     * @return
     */
    public String getQueryFromModel(Model model) {
        StmtIterator iter = model.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest));
        Statement currentStatement = null;
        Statement rdfsComment = null;
        String sparqlQuery = "";
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();
            rdfsComment = currentStatement.getSubject().getProperty(RDFS.comment);
            if (rdfsComment != null) {
                sparqlQuery = rdfsComment.getObject().toString();
                LOGGER.log(Level.INFO, "SPARQL Query found " + sparqlQuery);
                break;
            }
        }
        return sparqlQuery;
    }

    /**
     * Gets the result set as json string
     * @param resultSet
     * @return
     */
    public String getResultSetAsJsonString(ResultSet resultSet) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, resultSet);
        String jsonString = baos.toString();
        return jsonString;
    }
}

