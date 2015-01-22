package org.fiteagle.core.orchestrator.dm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IGeni;
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
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.mail.handlers.message_rfc822;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_ORCHESTRATOR),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrchestratorMDBListener implements MessageListener {

	private static Logger LOGGER = Logger
			.getLogger(OrchestratorMDBListener.class.toString());

	@Inject
	private JMSContext context;
	@javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	public void onMessage(final Message message) {
		String messageType = MessageUtil.getMessageType(message);
		String serialization = MessageUtil.getMessageSerialization(message);
		String messageBody = MessageUtil.getStringBody(message);
		LOGGER.log(Level.INFO, "Received a " + messageType + " message");

		if (messageType != null && messageBody != null) {
			if (messageType.equals(IMessageBus.TYPE_CONFIGURE)) {
				Model messageModel = MessageUtil.parseSerializedModel(
						messageBody, serialization);
				handleConfigureRequest(messageModel, serialization,
						MessageUtil.getJMSCorrelationID(message));
			} else if (messageType.equals(IMessageBus.TYPE_DELETE)) {
				Model messageModel = MessageUtil.parseSerializedModel(
						messageBody, serialization);
				handleDeleteRequest(messageModel, serialization,
						MessageUtil.getJMSCorrelationID(message));
			} else if (messageType.equals(IMessageBus.TYPE_INFORM)) {
				handleInform(messageBody,
						MessageUtil.getJMSCorrelationID(message));
			}
		}
	}

	private void handleInform(String body, String requestID) {

		if (requests.keySet().contains(requestID)) {

			LOGGER.log(Level.INFO, "Orchestrator received a reply");
			Model model = MessageUtil.parseSerializedModel(body, IMessageBus.SERIALIZATION_TURTLE);
			String requestType = requests.get(requestID).getRequestType();
			try {
				this.updateReservations(model, requests.get(requestID).getGroups(), requestType);

				TripletStoreAccessor.updateRepositoryModel(model);
				
				if (allInstancesHandled(requests.get(requestID).getGroups(), requestType)) {

					Model response = createResponse(requests.get(requestID).getGroups().keySet());
					sendResponse(requestID, response);
					requests.remove(requestID);
				}
			} catch (ResourceRepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void handleConfigureRequest(Model requestModel,
			String serialization, String requestID) {
		LOGGER.log(Level.INFO, "handling configure request ...");

		final Model modelCreate = ModelFactory.createDefaultModel();
		final Map<String, Group> groups = new HashMap<>();

		this.handleGroup(requestModel, modelCreate, groups, IGeni.GENI_PROVISIONED);
		this.handleReservation(requestModel, modelCreate, groups);

		String serializedModel = MessageUtil.serializeModel(modelCreate, IMessageBus.SERIALIZATION_TURTLE);
		LOGGER.log(Level.INFO, "message contains " + serializedModel);
		sendMessage(serializedModel, IMessageBus.TYPE_CREATE, IMessageBus.TARGET_ADAPTER, groups, requestID);

	}

	private void handleDeleteRequest(Model requestModel, String serialization, String requestID) {
		LOGGER.log(Level.INFO, "handling delete request ...");
		
		final Model modelDelete = ModelFactory.createDefaultModel();
		final Map<String, Group> groups = new HashMap<>();

		this.handleGroup(requestModel, modelDelete, groups, IGeni.GENI_UNALLOCATED);
		this.handleReservation(requestModel, modelDelete, groups);
		
		String serializedModel = MessageUtil.serializeModel(modelDelete, IMessageBus.SERIALIZATION_TURTLE);
		LOGGER.log(Level.INFO, "message contains " + serializedModel);
		sendMessage(serializedModel, IMessageBus.TYPE_DELETE, IMessageBus.TARGET_ADAPTER, groups, requestID);
		
	}

	private static Map<String, Request> requests = new HashMap<String, OrchestratorMDBListener.Request>();

	private void sendMessage(String model, String methodType,
			String methodTarget, Map<String, Group> groups,
			String initialRequestID) {

		final Message request = MessageUtil.createRDFMessage(model, methodType,
				methodTarget, IMessageBus.SERIALIZATION_TURTLE, null, context);
		Request r = new Request(groups, initialRequestID, methodType);
		requests.put(MessageUtil.getJMSCorrelationID(request), r);
		context.createProducer().send(topic, request);
		LOGGER.log(Level.INFO, methodType
				+ " message is sent to resource adapter");
	}

	private void sendResponse(String requestID, Model responseModel) {

		String serializedResponse = MessageUtil.serializeModel(responseModel,
				IMessageBus.SERIALIZATION_TURTLE);
		System.out.println("response model " + serializedResponse);
		Message responseMessage = MessageUtil.createRDFMessage(
				serializedResponse, IMessageBus.TYPE_INFORM, null,
				IMessageBus.SERIALIZATION_TURTLE, requests.get(requestID)
						.getRequestID(), context);
		LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
		context.createProducer().send(topic, responseMessage);
	}

	public static class Request {
		private Map<String, Group> groups;
		private String requestID;
		private String requestType;

		public Request(Map<String, Group> groups, String requestID, String requestType) {
			this.groups = groups;
			this.requestID = requestID;
			this.requestType = requestType;
		}

		protected Map<String, Group> getGroups() {
			return this.groups;
		}

		protected String getRequestID() {
			return this.requestID;
		}
		
		protected String getRequestType(){
			return this.requestType;
		}

	}

	public static class Group {
		private Map<String, ReservationDetails> reservations;

		public Group(Map<String, ReservationDetails> reservations) {
			this.reservations = reservations;
		}

		public Map<String, ReservationDetails> getReservations() {
			return this.reservations;
		}
		
		public void setReservations(String reservation, ReservationDetails reservationDetails){
			this.reservations.put(reservation, reservationDetails);
		}
	}

	public static class ReservationDetails {
		private String componentManagerId;
		private String status = "";

		public ReservationDetails(String componentManagerId) {
			this.componentManagerId = componentManagerId;
		}

		public String getComponentManangerId() {
			return this.componentManagerId;
		}

		public String getStatus() {
			return this.status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	private void handleGroup(Model requestModel, Model modelToBeSentToAdapter, Map<String, Group> groups, String state){
		
		StmtIterator groupIterator = requestModel.listStatements(null, RDF.type, MessageBusOntologyModel.classGroup);
		
		while (groupIterator.hasNext()) {
			
			Resource group = groupIterator.next().getSubject();
			LOGGER.log(Level.INFO,"trying to get reservations belonging to this URN " + group.getURI());

			try {
				final Map<String, ReservationDetails> reservations = this.getGroupReservations(group.getURI(), state);

				this.addToModelToBeSentToAdapter(reservations, modelToBeSentToAdapter);
				Group reservationsGroup = new Group(reservations);
				groups.put(group.getURI(), reservationsGroup);
			} catch (ResourceRepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void handleReservation(Model requestModel, Model modelToBeSentToAdapter, Map<String, Group> groups){
		
		StmtIterator reservationIterator = requestModel.listStatements(null, RDF.type, MessageBusOntologyModel.classReservation);
		while (reservationIterator.hasNext()) {
			Resource reservation = reservationIterator.next().getSubject();
			LOGGER.log(Level.INFO,
					"trying to get the reservation " + reservation.getURI());
			
			try {
				final Map<String, ReservationDetails> reservations = new HashMap<>();
				ReservationDetails reservationDetails = this.getReservationDetails(reservation.getURI());
				if(reservationDetails != null){
					reservations.put(reservation.getURI(), reservationDetails);
				}
				else {
					// null TODO: do sth
				}
				// TODO
				this.addToModelToBeSentToAdapter(reservations, modelToBeSentToAdapter);
				
				Group reservationsGroup = new Group(reservations);
				String groupURI = this.getGroupURI(reservation.getURI());
				groups.put(groupURI, reservationsGroup);
			} catch (ResourceRepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private String getGroupURI(String reservation) throws ResourceRepositoryException{
		String groupURI = "";
		String groupQuery = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "CONSTRUCT { "
				+ "?group a omn:Group ."
				+ " } "
				+ "FROM <http://localhost:3030/ds/query> "
				+ "WHERE {?group a omn:Group . "
				+ "?group omn:hasReservation \""
				+ reservation
				+ "\" . "
				+ "}";

		Model model = QueryExecuter.executeSparqlDescribeQuery(groupQuery);
		StmtIterator iter = model.listStatements();
		while(iter.hasNext()){
			groupURI = iter.next().getSubject().getURI();
		}
		return groupURI;
	}
	
	private final ReservationDetails getReservationDetails(
			String reservation) throws ResourceRepositoryException {

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?group ?componentManagerId WHERE { " + "<"
				+ reservation + "> a omn:Reservation . "
				+ "<" + reservation + "> omn:partOfGroup ?group . "
				+ "<" + reservation + "> omn:reserveInstanceFrom ?componentManagerId "
				+ "}";
		ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);

		if (rs.hasNext()) {
			QuerySolution qs = rs.next();

			if (qs.contains("group")
					&& qs.contains("componentManagerId")) {

				System.out.println("the sliver is found");
				System.out.println("reservation "
						+ " componentManagerId "
						+ qs.getLiteral("componentManagerId").getString());

				ReservationDetails reservationDetails = new ReservationDetails(
						qs.getLiteral("componentManagerId").getString());
				return reservationDetails;
			}
		}
		return null;
	}
	
	private final Map<String, ReservationDetails> getGroupReservations(
			String group, String requiredState) throws ResourceRepositoryException {
		final Map<String, ReservationDetails> reservations = new HashMap<>();

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?reservationId ?componentManagerId ?state WHERE { " + "<"
				+ group + "> a omn:Group ."
				+ "?reservationId omn:partOfGroup \"" + group + "\" . "
				+ "?reservationId omn:reserveInstanceFrom ?componentManagerId . "
				+ "?reservationId omn:hasState ?state "
				+ "}";
		ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);

		while (rs.hasNext()) {
			QuerySolution qs = rs.next();

			if (qs.contains("reservationId") && qs.contains("componentManagerId") && qs.contains("state")) {
				
				System.out.println("a sliver is found");
				System.out.println("reservation " + qs.getResource("reservationId").getURI()
						+ " componentManagerId " + qs.getLiteral("componentManagerId").getString() + " has state " + qs.getLiteral("state").getString());
				
				switch (requiredState){
				case IGeni.GENI_UNALLOCATED:
					// TODO : if(qs.getLiteral("state").getString().equals(IGeni.GENI_ALLOCATED)){}
					// TODO : if(qs.getLiteral("state").getString().equals(IGeni.GENI_UNALLOCATED)){}
					if(qs.getLiteral("state").getString().equals(IGeni.GENI_PROVISIONED)){
						ReservationDetails reservationDetails = new ReservationDetails(qs.getLiteral("componentManagerId").getString());
						reservations.put(qs.getResource("reservationId").getURI(), reservationDetails);
					}
					break;
				case IGeni.GENI_PROVISIONED:
					if(qs.getLiteral("state").getString().equals(IGeni.GENI_ALLOCATED)){
						ReservationDetails reservationDetails = new ReservationDetails(qs.getLiteral("componentManagerId").getString());
						reservations.put(qs.getResource("reservationId").getURI(), reservationDetails);
					}
					break;
				default:
					break;
				}
			}
		}
		return reservations;
	}
	
	
	private void addToModelToBeSentToAdapter( final Map<String, ReservationDetails> reservations,
			final Model modelCreate) throws ResourceRepositoryException {

		for (Map.Entry<String, ReservationDetails> instance : reservations.entrySet()) {

			String componentManagerId = instance.getValue().getComponentManangerId().toString();
			Resource resourceAdapter = modelCreate.createResource(componentManagerId);

			resourceAdapter.addProperty(RDF.type, this.getResourceAdapterName(componentManagerId));
			Resource resource = modelCreate.createResource(instance.getKey());
			resource.addProperty(RDF.type, this.getResourceName(componentManagerId));
		}
	}
	
	private Resource getResourceAdapterName(Object componentManagerId)
			throws ResourceRepositoryException {

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?resourceAdapter WHERE { " + "<" + componentManagerId
				+ "> a ?resourceAdapter ."
				+ "?resourceName omn:implementedBy ?resourceAdapter" + "}";

		ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);
		Resource resourceName = null;

		while (rs.hasNext()) {

			QuerySolution qs = rs.next();
			resourceName = qs.getResource("resourceAdapter");
		}
		return resourceName;
	}
	
	private Resource getResourceName(Object componentManangerId)
			throws ResourceRepositoryException {

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?resourceName WHERE { " + "<" + componentManangerId
				+ "> a ?class ." + "?resourceName omn:implementedBy ?class "
				+ "}";

		ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);
		Resource resourceName = null;

		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			resourceName = qs.getResource("resourceName");
		}
		return resourceName;
	}

	private void updateReservations(Model model, Map<String, Group> groups, String requestType)
			throws ResourceRepositoryException {

		StmtIterator iterator = model.listStatements();
		final List<String> reservationsURI = new LinkedList<>();

		while (iterator.hasNext()) {

			Resource resourceInstance = iterator.next().getSubject();
			String instance = resourceInstance.getURI();

			if (!reservationsURI.contains(instance)) {
				reservationsURI.add(instance);
			}
		}

		for (String reservationURI : reservationsURI) {

			for (Map.Entry<String, Group> group : groups.entrySet()) {

				if (group.getValue().getReservations().keySet()
						.contains(reservationURI)) {

					changeReservationState(model, reservationURI, requestType);
					
					switch (requestType) {
					case IMessageBus.TYPE_CREATE:
						group.getValue().getReservations().get(reservationURI).setStatus(IGeni.GENI_PROVISIONED);
						break;
					case IMessageBus.TYPE_DELETE:
						group.getValue().getReservations().get(reservationURI).setStatus(IGeni.GENI_UNALLOCATED);
						break;
					default:
						break;
					}
					
					break;
				}
			}
		}
	}
	
	private void changeReservationState(Model model,
			String reservationURI, String requestType) throws ResourceRepositoryException {

		Model deleteModel = ModelFactory.createDefaultModel();
		Resource deleteResource = deleteModel.createResource(reservationURI);
		TripletStoreAccessor.removePropertyValue(deleteResource,
				MessageBusOntologyModel.hasState);
		
		
		switch (requestType){
		case IMessageBus.TYPE_CREATE:
			Resource provisionedState = model.createResource(reservationURI);
			provisionedState.addProperty(MessageBusOntologyModel.hasState,IGeni.GENI_PROVISIONED);
			break;
			
		case IMessageBus.TYPE_DELETE:
			Resource reservation = model.createResource(reservationURI);
			model.remove(reservation, MessageBusOntologyModel.hasState, reservation.getProperty(MessageBusOntologyModel.hasState).getObject());
			
			Resource deletedState = model.createResource(reservationURI);
			deletedState.addProperty(MessageBusOntologyModel.hasState,IGeni.GENI_UNALLOCATED);
			break;
		default:
			break;
		}
		
	}
	
	private boolean allInstancesHandled(Map<String, Group> groups, String requestType) {

		boolean provisionFinished = true;

		for (Map.Entry<String, Group> group : groups.entrySet()) {

			for (Map.Entry<String, ReservationDetails> reservation : group
					.getValue().getReservations().entrySet()) {
				
				if(requestType.equals(IMessageBus.TYPE_CREATE)){
					if (!reservation.getValue().getStatus().equals(IGeni.GENI_PROVISIONED)) {
						provisionFinished = false;
						break;
					}
				} else if(requestType.equals(IMessageBus.TYPE_DELETE)){
					if (!reservation.getValue().getStatus().equals(IGeni.GENI_UNALLOCATED)) {
						provisionFinished = false;
						break;
					}
				}
			}
			if (!provisionFinished) {
				break;
			}
		}
		return provisionFinished;
	}
	
	private Model createResponse(Set<String> groupsURN)
			throws ResourceRepositoryException {

		Model responseModel = ModelFactory.createDefaultModel();
		Iterator<String> iterator = groupsURN.iterator();

		while (iterator.hasNext()) {

			String groupURN = iterator.next().toString();

			String reservationQuery = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
					+ "DESCRIBE ?reservationIDs WHERE {"
					+ "?reservationIDs a omn:Reservation . "
					+ "?reservationIDs omn:partOfGroup \""
					+ groupURN
					+ "\" . "
					+ "}";

			responseModel.add(QueryExecuter.executeSparqlDescribeQuery(reservationQuery));

		}

		return responseModel;
	}


}
