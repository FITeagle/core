package org.fiteagle.core.orchestrator;

import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import info.openmultinet.ontology.vocabulary.Omn_service;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.orchestrator.dm.OrchestratorStateKeeper;
import org.fiteagle.core.orchestrator.dm.Request;
import org.fiteagle.core.orchestrator.dm.RequestContext;
import org.fiteagle.api.core.TimeHelperMethods;
import org.fiteagle.api.core.TimeParsingException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
//import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
//import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

/**
 * Created by dne on 12.02.15.
 */
@Stateless
public class RequestHandler {
	@Inject
	OrchestratorStateKeeper stateKeeper;

	private static Logger LOGGER = Logger.getLogger(RequestHandler.class
			.toString());

	public void parseModel(RequestContext context, Model requestModel,
			String method) {

		Model requestedResources = this.getRequestedResources(requestModel,
				method);

		ResIterator resIterator = requestedResources
				.listSubjectsWithProperty(Omn_lifecycle.implementedBy);

		while (resIterator.hasNext()) {
			Resource requestedResource = resIterator.nextResource();
			String target = requestedResource
					.getProperty(Omn_lifecycle.implementedBy).getObject()
					.asResource().getURI();
			Request request = context.getRequestByTarget(target);
			request.setMethod(method);
			request.addOrUpdate(requestedResource);
			stateKeeper.addRequest(request);
		}
	}

	private Model getRequestedResources(Model requestModel, String method) {
		Model returnModel = ModelFactory.createDefaultModel();

		String modelString1 = MessageUtil.serializeModel(requestModel,
				IMessageBus.SERIALIZATION_TURTLE);

		ResIterator resIterator = requestModel.listSubjectsWithProperty(
				RDF.type, Omn.Resource);

		if (!resIterator.hasNext()) {
			ResIterator resIterator1 = requestModel.listSubjectsWithProperty(
					RDF.type, Omn.Topology);



			while (resIterator1.hasNext()) {

				Resource topo = resIterator1.nextResource();

				Model topoModel = TripletStoreAccessor.getResource(topo
						.getURI());

				String modelString = MessageUtil.serializeModel(topoModel,
						IMessageBus.SERIALIZATION_TURTLE);

				requestModel.add(topoModel);
				resIterator = requestModel.listSubjectsWithProperty(
						Omn.isResourceOf, topo);
				while (resIterator.hasNext()) {
					Resource resource = resIterator.nextResource();
					Statement keyStatement = topo
							.getProperty(Omn_service.publickey);
					if (keyStatement != null)
						resource.addProperty(keyStatement.getPredicate(),
								keyStatement.getObject());

					Statement username = topo.getProperty(Omn_service.username);
					if (username != null)
						resource.addProperty(username.getPredicate(),
								username.getObject());

				}
				resIterator = requestModel.listSubjectsWithProperty(
						Omn.isResourceOf, topo);

			}

		} else {

		}

		while (resIterator.hasNext()) {
			Resource requestedResource = resIterator.nextResource();
			Model resourceModel = TripletStoreAccessor
					.getResource(requestedResource.getURI());

			if (requestModel.contains(requestedResource, Omn_service.publickey)
					&& requestModel.contains(requestedResource,
							Omn_service.username)) {
				Statement publicKey = requestModel.getProperty(
						requestedResource, Omn_service.publickey);
				resourceModel.add(publicKey);
				Statement username = requestModel.getProperty(
						requestedResource, Omn_service.username);
				resourceModel.add(username);
			}

			// provision only resources with reservationState "Allocated"
			if (resourceModel.contains(requestedResource, Omn.hasReservation)) {
				Resource reservation = resourceModel
						.getProperty(requestedResource, Omn.hasReservation)
						.getObject().asResource();
				Model reservationModel = TripletStoreAccessor
						.getResource(reservation.getURI());
				String reservationState = reservationModel
						.getProperty(reservation,
								Omn_lifecycle.hasReservationState).getObject()
						.asResource().getURI();

				switch (method) {
				case IMessageBus.TYPE_CONFIGURE:
					addConfigurations(resourceModel, requestModel);
					returnModel.add(resourceModel);
					returnModel.add(requestedResource.getModel());

					break;
				case IMessageBus.TYPE_CREATE:

					if (reservationState.equals(Omn_lifecycle.Allocated
							.getURI())) {
						returnModel.add(resourceModel);
					}
					break;

				case IMessageBus.TYPE_DELETE:

					if (reservationState.equals(Omn_lifecycle.Provisioned
							.getURI())
							|| reservationState.equals(Omn_lifecycle.Allocated
									.getURI())) {
						returnModel.add(resourceModel);
					}
					break;

				default:
					returnModel.add(resourceModel);

				}

			} else {
				LOGGER.info("No reservation");
			}

		}
		return returnModel;

	}

	private void addConfigurations(Model resourceModel, Model requestModel) {

		Statement adapterInstanceStatement = resourceModel.getProperty(
				(Resource) null, Omn_lifecycle.implementedBy);
		Resource adapterInstance = adapterInstanceStatement.getObject()
				.asResource();
		if (requestModel.contains(adapterInstance, null)) {
			StmtIterator iter = requestModel.listStatements(new SimpleSelector(
					adapterInstance, null, (RDFNode) null));
			while (iter.hasNext()) {
				Statement stmt = iter.nextStatement();
				resourceModel.add(adapterInstanceStatement.getSubject(),
						stmt.getPredicate(), stmt.getObject());
			}
		}
	}

	public String isValidURN(Model requestModel) {
		String error_message = "";
		ResIterator resIterator = requestModel.listSubjectsWithProperty(
				RDF.type, Omn.Resource);
		if (!resIterator.hasNext()) {
			ResIterator resIterator1 = requestModel.listSubjectsWithProperty(
					RDF.type, Omn.Topology);
			error_message = checkURN(resIterator1, error_message);
		}
		error_message = checkURN(resIterator, error_message);
		return error_message;
	}

	private String checkURN(ResIterator resIterator, String error_message) {
		while (resIterator.hasNext()) {
			Resource resource = resIterator.nextResource();
			Model model = TripletStoreAccessor.getResource(resource.getURI());
			if (model.isEmpty() || model == null) {
				error_message += resource.getURI()
						+ " is not a valid urn. Please execute first allocate successfully.";
			}
		}
		return error_message;
	}

	protected void setStateKeeper(OrchestratorStateKeeper stateKeeper) {
		this.stateKeeper = stateKeeper;
	}

	public String checkValidity(Model messageModel) {
		String error_message = isValidURN(messageModel);
		if (error_message == null || error_message.isEmpty()) {
			error_message = checkTimes(messageModel);
		}
		return error_message;
	}

	private String checkTimes(Model messageModel) {
		String error_message = null;

		String modelString = MessageUtil.serializeModel(messageModel,
				IMessageBus.SERIALIZATION_TURTLE);

		ResIterator resIterator1 = messageModel.listSubjectsWithProperty(
				RDF.type, Omn.Topology);

		Resource resource = null;
		Model topologyModel = null;
		if (resIterator1.hasNext()) {
			resource = resIterator1.next();
			topologyModel = TripletStoreAccessor.getResource(resource.getURI());
		}

		boolean provisionTimeIsCurrent = false;

		if (topologyModel != null) {
			// a slice (topology) is to be provisioned
			String modelString1 = MessageUtil.serializeModel(topologyModel,
					IMessageBus.SERIALIZATION_TURTLE);

			ResIterator resIterator2 = topologyModel.listSubjectsWithProperty(
					RDF.type, Omn.Topology);

			Resource topologyResource = null;
			if (resIterator2.hasNext()) {
				topologyResource = resIterator2.next();
				String startTime = topologyResource
						.getProperty(MessageBusOntologyModel.startTime)
						.getObject().asLiteral().getString();
				String endTime = topologyResource
						.getProperty(MessageBusOntologyModel.endTime)
						.getObject().asLiteral().getString();
				Date currentTime = new Date();

				try {
					provisionTimeIsCurrent = TimeHelperMethods.timesOverlap(
							TimeHelperMethods.getTimeFromString(startTime),
							TimeHelperMethods.getTimeFromString(endTime),
							currentTime, currentTime);
				} catch (TimeParsingException e) {
					// TODO Auto-generated catch block
					LOGGER.log(Level.SEVERE, e.getMessage());
				}
			}
		} else {
			// a sliver (resource) is to be provisioned
			ResIterator resIterator2 = messageModel.listSubjectsWithProperty(
					RDF.type, Omn.Resource);

			Resource sliverResource = null;
			Model sliverModel = null;
			if (resIterator2.hasNext()) {
				sliverResource = resIterator2.next();
				sliverModel = TripletStoreAccessor.getResource(sliverResource
						.getURI());
			}

			String sliverModelString = MessageUtil.serializeModel(sliverModel,
					IMessageBus.SERIALIZATION_TURTLE);
			LOGGER.info("checkTimes sliver model: " + sliverModelString);

			// get topology, then get times
			NodeIterator sliverTopology = sliverModel
					.listObjectsOfProperty(Omn.isResourceOf);
			RDFNode topologyNode = null;
			if (sliverTopology.hasNext()) {
				topologyNode = sliverTopology.next();
			}

			Model reservationModel = null;
			if (topologyNode != null) {
				reservationModel = TripletStoreAccessor
						.getResource(topologyNode.asResource().getURI());
			}

			if (reservationModel != null) {
				String modelString3 = MessageUtil.serializeModel(
						reservationModel, IMessageBus.SERIALIZATION_TURTLE);

				ResIterator resIterator3 = reservationModel
						.listSubjectsWithProperty(RDF.type, Omn.Topology);

				Resource topologyResource2 = null;
				if (resIterator3.hasNext()) {
					topologyResource2 = resIterator3.next();
					String startTime = topologyResource2
							.getProperty(MessageBusOntologyModel.startTime)
							.getObject().asLiteral().getString();
					String endTime = topologyResource2
							.getProperty(MessageBusOntologyModel.endTime)
							.getObject().asLiteral().getString();
					Date currentTime = new Date();

					try {
						provisionTimeIsCurrent = TimeHelperMethods
								.timesOverlap(TimeHelperMethods
										.getTimeFromString(startTime),
										TimeHelperMethods
												.getTimeFromString(endTime),
										currentTime, currentTime);
					} catch (TimeParsingException e) {
						LOGGER.log(Level.SEVERE, e.getMessage());
					}
				}
			}
		}

		if (!provisionTimeIsCurrent) {
			error_message = "Resource can only be provisioned within the reserved time window.";
		}

		return error_message;
	}

}
