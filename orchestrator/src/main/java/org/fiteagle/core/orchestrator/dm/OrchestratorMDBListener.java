package org.fiteagle.core.orchestrator.dm;

import java.util.HashMap;
import java.util.Map;
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
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_ORCHESTRATOR),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrchestratorMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(OrchestratorMDBListener.class.toString());
  private static String OMN = "http://open-multinet.info/ontology/omn#";
  
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
        if (messageType.equals(IMessageBus.TYPE_CREATE)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleCreateRequest(messageModel, serialization, message.getJMSCorrelationID());
        }
        if (messageType.equals(IMessageBus.TYPE_DELETE)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleReleaseRequest(messageModel, serialization, message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleCreateRequest(Model requestModel, String serialization, String requestID)
      throws JMSException {
    LOGGER.log(Level.INFO, "handling provision request ...");
    Message responseMessage = null;
    Model resultModel = ModelFactory.createDefaultModel();

    StmtIterator iterator = requestModel.listStatements(null, RDF.type, OMN + "SLICE");
    while(iterator.hasNext()){
      Resource slice = iterator.next().getSubject();
      LOGGER.log(Level.INFO, "trying to provision this URN " + slice.getURI());
    }
    
    String serializedResponse = MessageUtil.serializeModel(resultModel);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization,
        requestID, context);
    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    context.createProducer().send(topic, responseMessage);
  }
  
  private void handleReleaseRequest(Model requestModel, String serialization, String requestID)
      throws JMSException {
    LOGGER.log(Level.INFO, "handling provision request ...");
//    Message responseMessage = null;
//    Model resultModel = ModelFactory.createDefaultModel();

   
    
//    String serializedResponse = MessageUtil.serializeModel(resultModel);
//    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization,
//        requestID, context);
//    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
//    context.createProducer().send(topic, responseMessage);
  }
  
}
