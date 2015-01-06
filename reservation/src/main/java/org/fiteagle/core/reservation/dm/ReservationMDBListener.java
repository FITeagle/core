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
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ReservationMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ReservationMDBListener.class.toString());
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
    Message responseMessage = null;
    Model resultModel = ModelFactory.createDefaultModel();
    final Map<String, String> reservedSlivers = new HashMap<>();
    this.handleReservation(requestModel, reservedSlivers);
    this.reserve(requestModel);
    
    for (Map.Entry<String, String> sliver : reservedSlivers.entrySet()) {
      Resource sliversResource = resultModel.createResource(sliver.getKey());
      sliversResource.addProperty(resultModel.createProperty(OMN + "reservation_status"), sliver.getValue());
    }
    String serializedResponse = MessageUtil.serializeModel(resultModel);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization,
        requestID, context);
    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    context.createProducer().send(topic, responseMessage);
  }
  
  private void handleReservation(Model requestModel, Map<String, String> reservedSlivers) {
    
    StmtIterator iterator = requestModel.listStatements(null, RDF.type, OMN + "sliver");
    LOGGER.log(Level.INFO, "handle reservation ...");
    while (iterator.hasNext()) {
      Resource sliver = iterator.next().getSubject();
      Statement st = sliver.getProperty(requestModel.createProperty(OMN + "reserve_Instance_from"));
      String componentManagerId = st.getObject().toString();
      LOGGER.log(Level.INFO, "componentManagerId " + componentManagerId);
      if (this.isReservationAvailable(componentManagerId)) {
        LOGGER.log(Level.INFO, "reservation is available");
        LOGGER.log(Level.INFO, "sliver is " + sliver.getURI());
        reservedSlivers.put(sliver.getURI(), "geni_allocated");
      } else {
        requestModel.remove(sliver, null, null);
        reservedSlivers.put(sliver.getURI(), "geni_not_allocated");
      }
    }
  }
  
  private boolean isReservationAvailable(String componentManagerId) {
    boolean result = false;
    String query = createSPARQLquery(componentManagerId);
    try {
      ResultSet result_set = QueryExecuter.executeSparqlSelectQuery(query);
      result = checkRequestResult(result_set);
    } catch (ResourceRepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
  }
  
  private String createSPARQLquery(String componentManagerId) {
    String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
        + "SELECT ?maxInstances ?reservedInstances ?createdInstances WHERE {"
        + "OPTIONAL { ?instance a ?resourceType ." + " <" + componentManagerId + "> a ?adapterType ."
        + "?adapterType omn:implements ?resourceType .}" + "OPTIONAL { <" + componentManagerId
        + "> omn:maxInstances ?maxInstances  }" + "OPTIONAL { ?reservedInstances omn:reserve_Instance_from <"
        + componentManagerId + "> } " + "}";
    return query;
  }
  
  private boolean checkRequestResult(ResultSet result) {
    int maxInstances = 0;
    int createdInstances = 0;
    int reservedInstances = 0;
    while (result.hasNext()) {
      QuerySolution qs = result.next();
      if (qs.contains("maxInstances")) {
        maxInstances = qs.getLiteral("maxInstances").getInt();
      }
      if (qs.contains("?createdInstances")) {
        createdInstances = createdInstances + 1;
      }
      if (qs.contains("reservedInstances")) {
        reservedInstances = reservedInstances + 1;
      }
    }
    if (maxInstances != 0) {
      if (maxInstances - (createdInstances + reservedInstances) > 0) {
        return true;
      } else
        return false;
    }
    return true;
  }
  
  private void reserve(Model model) {
    
    try {
      TripletStoreAccessor.updateRepositoryModel(model);
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
}