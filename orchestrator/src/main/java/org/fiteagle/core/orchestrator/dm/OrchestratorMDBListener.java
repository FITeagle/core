package org.fiteagle.core.orchestrator.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.orchestrator.provision.HandleProvision;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
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
			Model model = MessageUtil.parseSerializedModel(body,
					IMessageBus.SERIALIZATION_TURTLE);

			try {
				HandleProvision.updateReservations(model,
						requests.get(requestID).getGroups());

				TripletStoreAccessor.updateRepositoryModel(model);

				if (HandleProvision.allInstancesProvisioned(requests.get(
						requestID).getGroups())) {

					Model provisionResponse = HandleProvision
							.createProvisionResponse(requests.get(requestID)
									.getGroups().keySet());

					sendResponse(requestID, provisionResponse);

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
		Map<String, Group> groups = new HashMap<>();

		StmtIterator iterator = requestModel.listStatements(null, RDF.type,
				MessageBusOntologyModel.classGroup);
		while (iterator.hasNext()) {
			Resource slice = iterator.next().getSubject();
			LOGGER.log(Level.INFO,
					"trying to provision this URN " + slice.getURI());

			try {
				final Map<String, ReservationDetails> reservations = HandleProvision
						.getReservations(slice.getURI());

				HandleProvision.addToCreateRequest(reservations, modelCreate);
				Group reservationsGroup = new Group(reservations);
				groups.put(slice.getURI(), reservationsGroup);
			} catch (ResourceRepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String serializedModel = MessageUtil.serializeModel(modelCreate,
				IMessageBus.SERIALIZATION_TURTLE);
		LOGGER.log(Level.INFO, "message contains " + serializedModel);
		sendMessage(serializedModel, IMessageBus.TYPE_CREATE,
				IMessageBus.TARGET_ADAPTER, groups, requestID);

	}

	private void handleDeleteRequest(Model requestModel, String serialization,
			String requestID) {
		LOGGER.log(Level.INFO, "handling delete request ...");
	}

	private static Map<String, Request> requests = new HashMap<String, OrchestratorMDBListener.Request>();

	private void sendMessage(String model, String methodType,
			String methodTarget, Map<String, Group> groups,
			String initialRequestID) {

		final Message request = MessageUtil.createRDFMessage(model, methodType,
				methodTarget, IMessageBus.SERIALIZATION_TURTLE, null, context);
		Request r = new Request(groups, initialRequestID);
		requests.put(MessageUtil.getJMSCorrelationID(request), r);
		context.createProducer().send(topic, request);
		LOGGER.log(Level.INFO, methodType
				+ " message is sent to resource adapter");
	}

	private void sendResponse(String requestID, Model responseModel) {

		String serializedResponse = MessageUtil.serializeModel(responseModel,
				IMessageBus.SERIALIZATION_TURTLE);
		System.out.println("provision response model " + serializedResponse);
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

		public Request(Map<String, Group> groups, String requestID) {
			this.groups = groups;
			this.requestID = requestID;
		}

		protected Map<String, Group> getGroups() {
			return this.groups;
		}

		protected String getRequestID() {
			return this.requestID;
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
	}

	public static class ReservationDetails {
		private String componentManagerId;
		private String status;

		public ReservationDetails(String componentManagerId, String status) {
			this.componentManagerId = componentManagerId;
			this.status = status;
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

}
