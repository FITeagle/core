package org.fiteagle.core.reservation.dm;

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
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ReservationMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ReservationMDBListener.class.toString());
  private static String componentManagerId = "component_manager_id";
  
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
        
        if (messageType.equals(IMessageBus.TYPE_REQUEST)) {
          Resource messageResource = messageModel.getResource(MessageBusOntologyModel.internalMessage.getURI());
          if (messageResource.getProperty(MessageBusOntologyModel.requestType) != null
              && messageResource.getProperty(MessageBusOntologyModel.requestType).getObject().toString()
                  .equals(IMessageBus.REQUEST_TYPE_RESERVE)) {
            LOGGER.log(Level.INFO, "Received a " + messageType + " message");
            handleRequest(messageModel, messageResource, serialization, message.getJMSCorrelationID());
          }
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleRequest(Model requestModel, Resource messageResource, String serialization, String requestID)
      throws JMSException {
    LOGGER.log(Level.INFO, "handling reservation request ...");
    // Message responseMessage = null;
    
    Map<String, String> allocateParameters = new HashMap<>();
    parseReservationParameters(allocateParameters, requestModel);
    
    if(isReservationAvailable(allocateParameters.get(componentManagerId))) {
      // to do: put reservation parameters in repository.
    }
    else {
      // to do: instance can't be reserved. 
    }
    // to do: send message back to SFA. 
    // context.createProducer().send(topic, responseMessage);
  }
  
  private void parseReservationParameters(Map<String, String> allocateParameters, Model requestModel){
    StmtIterator iterator = requestModel.listStatements();
    while (iterator.hasNext()) {
      Statement st = iterator.next();
      LOGGER.log(Level.INFO, st.getPredicate().getLocalName() + " " + st.getObject());
      allocateParameters.put(st.getPredicate().getLocalName().toString(), st.getObject().toString());
    }
  }
  
  private boolean isReservationAvailable(String componentManagerId){
     String query = createSPARQLquery(componentManagerId);
     try {
       ResultSet result_set  = QueryExecuter.executeSparqlSelectQuery(query);
       // to do: parse ResultSet
    } catch (ResourceRepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
     return true;
  }
  
 private String createSPARQLquery(String componentManagerId){
   String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
          + "SELECT ?maxInstances ?reservedInstances ?createdInstances WHERE {"
          + "OPTIONAL { ?instance a ?resourceType ."
          + " <" + componentManagerId + "> a ?adapterType ."
          + "?adapterType omn:implements ?resourceType .}"
          + "OPTIONAL { <" + componentManagerId + "> omn:maxInstances ?maxInstances  }"
          + "OPTIONAL { ?reservedInstances omn:reserve_Instance <" + componentManagerId + "> } "
          + "}";
   return query;
 }



}