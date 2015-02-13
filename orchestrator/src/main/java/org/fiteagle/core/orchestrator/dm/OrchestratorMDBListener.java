package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.*;
import com.sun.org.apache.xpath.internal.operations.Mod;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

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
import javax.xml.soap.MessageFactory;

import org.fiteagle.api.core.IGeni;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.orchestrator.ConfigurationHandler;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_ORCHESTRATOR),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrchestratorMDBListener implements MessageListener {

	private static Logger LOGGER = Logger
			.getLogger(OrchestratorMDBListener.class.toString());

    @Inject
    OrchestratorStateKeeper stateKeeper;

    @Inject
    ConfigurationHandler configurationHandler;


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
                try {
                    handleInform(messageBody,
                            MessageUtil.getJMSCorrelationID(message));
                } catch (ResourceRepositoryException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                }
            } else if(messageType.equals(IMessageBus.TYPE_CREATE)){
				LOGGER.log(Level.INFO, "Create Topology");
				handleCreateRequest(messageBody,MessageUtil.getJMSCorrelationID(message) );
			}else if(messageType.equals(IMessageBus.TYPE_GET)){
                handleGet(messageBody, MessageUtil.getJMSCorrelationID(message));
            }
		}
	}

    private void handleGet(String messageBody, String jmsCorrelationID) {
        Model model = MessageUtil.parseSerializedModel(messageBody, IMessageBus.SERIALIZATION_TURTLE);
        ResIterator resIterator = model.listSubjectsWithProperty(RDF.type,Omn.Resource);
        Model responseModel = ModelFactory.createDefaultModel();

        while (resIterator.hasNext()){
            Resource r = resIterator.nextResource();


                Model m = TripletStoreAccessor.getResource(r.getURI());
                Resource resource = m.getResource(r.getURI());
                Model reservation = TripletStoreAccessor.getResource(resource.getProperty(Omn.hasReservation).getObject().asResource().getURI());

                responseModel.add(m);
                responseModel.add(reservation);


        }
        sendResponse(jmsCorrelationID,responseModel);
    }

    private void handleCreateRequest(String messageBody, String jmsCorrelationID) {
		Model model = MessageUtil.parseSerializedModel(messageBody, IMessageBus.SERIALIZATION_TURTLE);

		ResIterator resIterator = model.listSubjectsWithProperty(RDF.type,Omn.Topology);

		while (resIterator.hasNext()){
			Resource r = resIterator.nextResource();

			try {
				TripletStoreAccessor.addResource(r);
				Model m = ModelFactory.createDefaultModel();
				Resource resource = m.createResource(r.getURI());
				sendResponse(jmsCorrelationID,m);

			} catch (ResourceRepositoryException e) {
				e.printStackTrace();
			}
		}

	}

	private void handleInform(String body, String requestID) throws ResourceRepositoryException {

        Request request = stateKeeper.getRequest(requestID);
        if(request!=null) {

			LOGGER.log(Level.INFO, "Orchestrator received a reply");
			Model model = MessageUtil.parseSerializedModel(body, IMessageBus.SERIALIZATION_TURTLE);

				RequestContext requestContext = request.getContext();
                request.setHandled();
            if(requestContext.allAnswersReceived()) {
                Model response = ModelFactory.createDefaultModel();

                for(Resource resource : request.getResourceList()) {

                    String topologyURI = resource.getProperty(Omn.isResourceOf).getObject().asResource().getURI();
                    Model top = TripletStoreAccessor.getResource(topologyURI);
                    response.add(top);
                    response.add(resource.listProperties());
                    Model reservationModel = TripletStoreAccessor.getResource(resource.getProperty(Omn.hasReservation).getObject().asResource().getURI());
                    Resource reservation = reservationModel.getResource(resource.getProperty(Omn.hasReservation).getObject().asResource().getURI());
                    TripletStoreAccessor.deleteModel(reservationModel);
                    reservation.removeAll(Omn_lifecycle.hasReservationState);
                    reservation.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Provisioned);
                    response.add(reservationModel);

                    TripletStoreAccessor.addModel(reservationModel);
                }
					sendResponse(requestContext.getRequestContextId(), response);
                stateKeeper.removeRequest(requestID);

				}

		}
	}

	private void handleConfigureRequest(Model requestModel,
			String serialization, String requestID) {
		LOGGER.log(Level.INFO, "handling configure request ...");

        RequestContext requestContext = new RequestContext(requestID);

        configurationHandler.parseModel(requestContext,requestModel);

        this.sendConfigureToResources(requestContext);


	}

    private void sendConfigureToResources(RequestContext requestContext) {

        Map<String, Request> requestMap = requestContext.getRequestMap();

        for(String requestId: requestMap.keySet()){
            configureResource(requestMap.get(requestId));
        }
    }

    private void configureResource(Request request) {

        Model requestModel = ModelFactory.createDefaultModel();
        Resource requestTopology = requestModel.createResource(Omn.Topology.getURI() + "/" + UUID.randomUUID());
        requestTopology.addProperty(RDF.type, Omn.Topology);

        for(Resource resource: request.getResourceList()){
            Model messageModel = TripletStoreAccessor.getResource(resource.getURI());
            requestTopology.addProperty(Omn.hasResource, messageModel.getResource(resource.getURI()));
            requestModel.add(resource.listProperties());
        }

        Model targetModel = TripletStoreAccessor.getResource(request.getTarget());
        String target = targetModel.getResource(request.getTarget()).getProperty(RDF.type).getObject().asResource().getURI();

        Message message = MessageUtil.createRDFMessage(requestModel,IMessageBus.TYPE_CREATE, target, IMessageBus.SERIALIZATION_TURTLE, request.getRequestId(), context);

        context.createProducer().send(topic, message);

    }

    private void handleDeleteRequest(Model requestModel, String serialization, String requestID) {
		LOGGER.log(Level.INFO, "handling delete request ...");
		
		final Model modelDelete = ModelFactory.createDefaultModel();
		final Map<String, Group> groups = new HashMap<>();

		this.handleGroup(requestModel, modelDelete, groups, Omn_lifecycle.Unallocated);
		this.handleReservation(requestModel, modelDelete, groups);
		
		String serializedModel = MessageUtil.serializeModel(modelDelete, IMessageBus.SERIALIZATION_TURTLE);
		LOGGER.log(Level.INFO, "message contains " + serializedModel);
		//sendMessage(serializedModel, IMessageBus.TYPE_DELETE, IMessageBus.TARGET_ADAPTER, groups, requestID);
		
	}

	private static Map<String, Request> requests = new HashMap<>();


	//private void sendMessage(String model, String methodType,
	//		String methodTarget, Map<String, Group> groups,
	//		String initialRequestID) {

//		final Message request = MessageUtil.createRDFMessage(model, methodType,
//				methodTarget, IMessageBus.SERIALIZATION_TURTLE, null, context);
//        //Request r = new Request(groups, initialRequestID, methodType);
//		requests.put(MessageUtil.getJMSCorrelationID(request), r);
//		context.createProducer().send(topic, request);
//		LOGGER.log(Level.INFO, methodType
//				+ " message is sent to resource adapter");
	//}

	private void sendResponse(String requestID, Model responseModel) {

		String serializedResponse = MessageUtil.serializeModel(responseModel,
				IMessageBus.SERIALIZATION_TURTLE);
		System.out.println("response model " + serializedResponse);
		Message responseMessage = MessageUtil.createRDFMessage(
				serializedResponse, IMessageBus.TYPE_INFORM, null,
				IMessageBus.SERIALIZATION_TURTLE,requestID, context);
		LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
		context.createProducer().send(topic, responseMessage);
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
		private OntClass status = null;

		public ReservationDetails(String componentManagerId) {
			this.componentManagerId = componentManagerId;
		}

		public String getComponentManangerId() {
			return this.componentManagerId;
		}

		public OntClass getStatus() {
			return this.status;
		}

		public void setStatus(OntClass status) {
			this.status = status;
		}
	}

	private void handleGroup(Model requestModel, Model modelToBeSentToAdapter, Map<String, Group> groups, OntClass state){
		
		StmtIterator groupIterator = requestModel.listStatements(null, RDF.type, Omn.Group);
		
		while (groupIterator.hasNext()) {
			
			Resource group = groupIterator.next().getSubject();
			LOGGER.log(Level.INFO,"trying to getInfrastructure reservations belonging to this URN " + group.getURI());

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
		
		StmtIterator reservationIterator = requestModel.listStatements(null, RDF.type, Omn.Resource);
		while (reservationIterator.hasNext()) {
			Resource reservation = reservationIterator.next().getSubject();
			LOGGER.log(Level.INFO,
					"trying to getInfrastructure the reservation " + reservation.getURI());
			
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
			String group, OntClass requiredState) throws ResourceRepositoryException {
		final Map<String, ReservationDetails> reservations = new HashMap<>();

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?reservationId ?componentManagerId ?state WHERE { " + "<"
				+ group + "> a omn:Group ."
				+ "?reservationId omn:isReservationOf \"" + group + "\" . "
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
				
				switch (requiredState.getLocalName()){
				case IGeni.GENI_UNALLOCATED: // TODO: musst be changed to Omn_lifecycle.Unallocated
					// TODO : if(qs.getLiteral("state").getString().equals(IGeni.GENI_ALLOCATED)){}
					// TODO : if(qs.getLiteral("state").getString().equals(IGeni.GENI_UNALLOCATED)){}
					if(qs.getLiteral("state").getString().equals(IGeni.GENI_UNALLOCATED)){
						ReservationDetails reservationDetails = new ReservationDetails(qs.getLiteral("componentManagerId").getString());
						reservations.put(qs.getResource("reservationId").getURI(), reservationDetails);
					}
					break;
				case IGeni.GENI_PROVISIONED: // should be changed to Omn_lifecycle.Provisiond
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
						group.getValue().getReservations().get(reservationURI).setStatus(Omn_lifecycle.Provisioned);
						break;
					case IMessageBus.TYPE_DELETE:
						group.getValue().getReservations().get(reservationURI).setStatus(Omn_lifecycle.Unallocated);
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
				Omn_lifecycle.hasReservationState);
		
		
		switch (requestType){
		case IMessageBus.TYPE_CREATE:
			Resource provisionedState = model.createResource(reservationURI);
			provisionedState.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Provisioned);
			break;
			
		case IMessageBus.TYPE_DELETE:
			Resource reservation = model.createResource(reservationURI);
			model.remove(reservation, Omn_lifecycle.hasReservationState, reservation.getProperty(Omn_lifecycle.hasReservationState).getObject());
			
			Resource deletedState = model.createResource(reservationURI);
			deletedState.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Unallocated);
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
	
	private Model createResponse(Map<String, Group> groups)
			throws ResourceRepositoryException {

		Model responseModel = ModelFactory.createDefaultModel();
		
		for(Map.Entry<String, Group> group : groups.entrySet()){
			
			for(Map.Entry<String, ReservationDetails> reservation : group.getValue().getReservations().entrySet()){
				
				String reservationQuery = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
						+ "DESCRIBE ?reservationIDs WHERE {"
						+ "?reservationIDs a omn:Reservation . "
						+ "?reservationIDs omn:partOfGroup \""
						+ group.getKey()
						+ "\" . "
						+ "?reservationIDs <http://www.w3.org/2002/07/owl#sameAs> <" + reservation.getKey() + "> "
						+ "}";
				
				responseModel.add(QueryExecuter.executeSparqlDescribeQuery(reservationQuery));
			}
		}

		return responseModel;
	}


}
