package org.fiteagle.core.reservation.dm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import com.hp.hpl.jena.rdf.model.RDFList;
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
  private static String componentManagerId = "component_manager_id";
  private static String URN = "urn";
  private static String EndTime = "endTime";
  
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
    final List<String> sliversList = new LinkedList<>();
    final Map<String, String> allocateParameters = new HashMap<>();
    
    this.parseReservationParameters(allocateParameters, requestModel);
    if (isReservationAvailable(allocateParameters.get(componentManagerId))) {
      Model reservationModel = this.createReserveModel(allocateParameters, sliversList);
      this.reserve(reservationModel);
    } else {
      // to do: instance can't be reserved.
    }
    if(!sliversList.isEmpty()){
      LOGGER.log(Level.INFO, " resevation is done ");
      RDFList rdfList = resultModel.createList();
      for (String createdSlivers : sliversList){ 
        rdfList.addProperty(resultModel.createProperty(createdSlivers), "sliver");
      }
    }
    String serializedResponse = MessageUtil.serializeModel(resultModel);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization,
        requestID, context);
    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    context.createProducer().send(topic, responseMessage);
  }
  
  private void parseReservationParameters(Map<String, String> allocateParameters, Model requestModel) {
    StmtIterator iterator = requestModel.listStatements();
    while (iterator.hasNext()) {
      Statement st = iterator.next();
      LOGGER.log(Level.INFO, st.getPredicate().getLocalName() + " " + st.getObject());
      allocateParameters.put(st.getPredicate().getLocalName().toString(), st.getObject().toString());
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
  
  private Model createReserveModel( Map<String, String> allocateParameters, List<String> sliversList){
    Model reservationModel = ModelFactory.createDefaultModel();
    String omn = "http://open-multinet.info/ontology/omn#";
    
    Resource slice = reservationModel.createResource(allocateParameters.get(URN));
    slice.addProperty(RDF.type, omn + "slice");
    if(allocateParameters.containsKey(EndTime)){
      slice.addProperty(reservationModel.createProperty(omn + EndTime), allocateParameters.get(EndTime));
    }
    
    Resource sliver = reservationModel.createResource(allocateParameters.get(URN) + "+sliver"); // to be changed.
    sliver.addProperty(RDF.type, omn + "sliver");
    sliver.addProperty(reservationModel.createProperty(omn + "PartOf"), slice.getURI());
    sliver.addProperty(reservationModel.createProperty(omn + "reserve_Instance_from"), allocateParameters.get(componentManagerId));
    sliversList.add(allocateParameters.get(URN) + "+sliver");
    return reservationModel;
  }
  
  private void reserve(Model model){
    
    try {
      TripletStoreAccessor.updateRepositoryModel(model);
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  

  
}