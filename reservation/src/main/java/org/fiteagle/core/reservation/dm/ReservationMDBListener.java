package org.fiteagle.core.reservation.dm;

import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import com.hp.hpl.jena.rdf.model.*;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_RESERVATION),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ReservationMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ReservationMDBListener.class.toString());
  private static String OMN = "http://open-multinet.info/ontology/omn#";
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  public void onMessage(final Message message) {
    String messageType = MessageUtil.getMessageType(message);
    String serialization = MessageUtil.getMessageSerialization(message);
    String rdfString = MessageUtil.getStringBody(message);
    LOGGER.log(Level.INFO, "Received a " + messageType + " message");
    
    if (messageType != null && rdfString != null) {      
      Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
      
      if (messageType.equals(IMessageBus.TYPE_CREATE)) {        
        try {
			handleCreate(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
		} catch (ResourceRepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      }
      if (messageType.equals(IMessageBus.TYPE_GET)) {
        handleGet(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
      }
    }
  }

  private void handleGet(Model messageModel, String serialization, String jmsCorrelationID) {
    Message responseMessage = null;
    Model resultModel = ModelFactory.createDefaultModel();

    //getInfrastructure Slice URN or Sliver URNS

    ResIterator iterator = messageModel.listResourcesWithProperty(RDF.type, Omn.Topology);
    if(iterator.hasNext()){
      //should be only one resource
      Resource r = iterator.nextResource();
      String uri =  r.getURI();
      String queryAssociatedReservations  ="";
      try {
         queryAssociatedReservations = buildQueryForGroupReservations(uri);
         ResultSet rs = QueryExecuter.executeSparqlSelectQuery(queryAssociatedReservations);
         while(rs.hasNext()){
           QuerySolution qs = rs.next();
           Resource result = qs.get("?reservationId").asResource();
           Resource resource = resultModel.createResource(result.getURI(), Omn.Resource);
           resource.addProperty(Omn_lifecycle.hasReservationState,qs.get("?state"));
           resource.addProperty(MessageBusOntologyModel.endTime, qs.get("?endTime"));

         }
      } catch (ResourceRepositoryException e) {
        e.printStackTrace();
      }
      System.out.println(queryAssociatedReservations);
    }else{
      iterator =  messageModel.listResourcesWithProperty(RDF.type, Omn.Resource);
      while(iterator.hasNext()){
        Resource r = iterator.nextResource();
        System.out.println("ssss");
      }
    }
    final Map<String, String> reservedSlivers = new HashMap<>();
    String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, jmsCorrelationID, context);
    context.createProducer().send(topic, responseMessage);
  }

  private String buildQueryForGroupReservations(String uri) throws ResourceRepositoryException {
    String query  = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "+
                    "SELECT ?reservationId ?state ?endTime WHERE {\n" +
            "?reservationId  omn:partOfGroup \""+uri+"\".\n" +
            "?reservationId omn:hasState ?state .\n" +
            "?reservationId omn:endTime  ?endTime\n" +
            "}";
    return query;

  }

  private void handleCreate(Model requestModel, String serialization, String requestID) throws ResourceRepositoryException {
    LOGGER.log(Level.INFO, "handling reservation request ...");
    Message responseMessage = null;
//    Model resultModel = ModelFactory.createDefaultModel();
    final Map<String, OntClass> reservedSlivers = new HashMap<>();
    Model reservationModel = this.handleReservation(requestModel, reservedSlivers);
    this.reserve(reservationModel);
    
//    for (Map.Entry<String, OntClass> sliver : reservedSlivers.entrySet()) {
//      Resource sliversResource = resultModel.createResource(sliver.getKey());
//      sliversResource.addProperty(Omn_lifecycle.hasReservationState, sliver.getValue());
//    }
//    String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
    String serializedResponse = MessageUtil.serializeModel(reservationModel, serialization);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    context.createProducer().send(topic, responseMessage);
  }
  
  private Model handleReservation(Model requestModel, Map<String, OntClass> reservedSlivers) throws ResourceRepositoryException {
    
    LOGGER.log(Level.INFO, "handle reservation ...");
    Model reservationModel = ModelFactory.createDefaultModel();
    
   ResIterator iterator = requestModel.listResourcesWithProperty(RDF.type, Omn.Topology);
    while (iterator.hasNext()) {
        Resource topology = iterator.nextResource();
        if (TripletStoreAccessor.exists(topology.getURI())) {
            LOGGER.log(Level.INFO, "Topology already exists");
            Model topologyModel = TripletStoreAccessor.getResource(topology.getURI());
            topologyModel.add(requestModel);


            reservationModel = topologyModel;
        }else{
            LOGGER.log(Level.INFO, "Topology does not exists.");

            TripletStoreAccessor.addModel(requestModel);
            reservationModel =requestModel;
        }



//      Statement st = sliver.getProperty(Omn.isResourceOf);
//      String componentManagerId = st.getObject().toString();
//      LOGGER.log(Level.INFO, "componentManagerId " + componentManagerId);
//      if (this.isReservationAvailable(componentManagerId)) {
//        LOGGER.log(Level.INFO, "reservation is available");
//        String sliverURN = setSliverURN(sliver.getURI());
//        reservedSlivers.put(sliverURN, Omn_lifecycle.Allocated);
//        addSliverURNtoReservationModel(reservationModel, requestModel, sliverURN, sliver.getURI());
//      } else {
//        reservedSlivers.put(sliver.getURI(), Omn_lifecycle.Unallocated);
//      }
//    }
//    addSliceURNtoReservationModel(reservationModel, requestModel, reservedSlivers);
    }
    return reservationModel;
  }



    private void addSliceURNtoReservationModel(Model reservationModel, Model requestModel, Map<String, OntClass> reservedSlivers){
	  StmtIterator iterator = requestModel.listStatements(null, RDF.type, Omn.Group);
	  Resource slice = null;
	  while(iterator.hasNext()){
		  Statement statement = iterator.next();
		  slice = statement.getSubject();
	  }
	  Resource sliceURN = reservationModel.createResource(slice.getURI());
	  StmtIterator iter = slice.listProperties();
	  while(iter.hasNext()){
		  Statement statement = iter.next();
		  sliceURN.addProperty(statement.getPredicate(), statement.getObject());
	  }
	  for(Map.Entry<String, OntClass> slivers : reservedSlivers.entrySet()){
		  if(slivers.getValue().equals(Omn_lifecycle.Allocated)){
			  sliceURN.addProperty(Omn.hasReservation, slivers.getKey());  
		  }
	  }
  }
  
  private void addSliverURNtoReservationModel(Model reservationModel, Model requestModel, String sliverURN, String sliver){
	  Resource newSliverURN = reservationModel.createResource(sliverURN);
	  StmtIterator iterator = requestModel.listStatements();
	  while (iterator.hasNext()) {
		  Statement statement = iterator.next();
		  Resource helpingResource = statement.getSubject();
		  if(helpingResource.getURI().equals(sliver)){
			  newSliverURN.addProperty(statement.getPredicate(), statement.getObject());
		  }
	  }
  }
  
  private String setSliverURN(String sliver) throws ResourceRepositoryException{
	  String sliverURN = "";
	  Random random = new Random();
	  while(true){
		  sliverURN = sliver + random.nextInt();
		  if(!sliverURNallocated(sliverURN)){
			  break;
		  }
	  }
	  return sliverURN;
  }
  
  private boolean sliverURNallocated(String sliverURN) throws ResourceRepositoryException{
	String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
	  		+ "ASK WHERE { <" + sliverURN + "> a omn:Reservation }";
	return QueryExecuter.executeSparqlAskQuery(query);
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
        + "PREFIX omnLifecycle: <http://open-multinet.info/ontology/omn-lifecycle#> "
        + "SELECT ?maxInstances ?reservedInstances ?createdInstances WHERE {"
        + "OPTIONAL { ?instance a ?resourceType ." + " <" + componentManagerId + "> a ?adapterType ."
        + "?adapterType omnLifecycle:implements ?resourceType .}" + "OPTIONAL { <" + componentManagerId
        + "> omn:maxInstances ?maxInstances  }" + "OPTIONAL { ?reservedInstances omn:isResourceOf <"
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

      ResIterator resIterator = model.listResourcesWithProperty(Omn.isResourceOf);
      while(resIterator.hasNext()){
          Resource resource = resIterator.nextResource();
          Resource reservation = model.createResource(Omn.Reservation + "/"+ UUID.randomUUID().toString());
          reservation.addProperty(RDF.type,Omn.Reservation);
          resource.addProperty(Omn.hasReservation, reservation);
          reservation.addProperty(Omn.isReservationOf, resource);
          Date afterAdding2h = getDefaultExpirationTime();
          reservation.addProperty(MessageBusOntologyModel.endTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(afterAdding2h));
          reservation.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Allocated);
      }

      try {
          TripletStoreAccessor.addModel(model);


    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }

    private static Date getDefaultExpirationTime() {
        Date date = new Date();
        long t=date.getTime();
        return new Date(t + (120 * 60000));
    }
  
}
