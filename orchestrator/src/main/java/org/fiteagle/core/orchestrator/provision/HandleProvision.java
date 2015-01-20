package org.fiteagle.core.orchestrator.provision;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fiteagle.api.core.IGeni;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.fiteagle.core.orchestrator.dm.OrchestratorMDBListener.ReservationDetails;
import org.fiteagle.core.orchestrator.dm.OrchestratorMDBListener.Group;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class HandleProvision {

	public static final Map<String, ReservationDetails> getGroupReservations(
			String group) throws ResourceRepositoryException {
		final Map<String, ReservationDetails> reservations = new HashMap<>();

		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
				+ "SELECT ?reservationId ?componentManagerId WHERE { " + "<"
				+ group + "> a omn:Group ."
				+ "?reservationId omn:partOfGroup \"" + group + "\" . "
				+ "?reservationId omn:reserveInstanceFrom ?componentManagerId "
				+ "}";
		ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);

		while (rs.hasNext()) {
			QuerySolution qs = rs.next();

			if (qs.contains("reservationId")
					&& qs.contains("componentManagerId")) {

				System.out.println("a sliver is found");
				System.out.println("reservation "
						+ qs.getResource("reservationId").getURI()
						+ " componentManagerId "
						+ qs.getLiteral("componentManagerId").getString());

				ReservationDetails reservationDetails = new ReservationDetails(
						qs.getLiteral("componentManagerId").getString(),
						IGeni.GENI_ALLOCATED);
				reservations.put(qs.getResource("reservationId").getURI(),
						reservationDetails);
			}
		}
		return reservations;
	}
	
	public static final ReservationDetails getReservationDetails(
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
						qs.getLiteral("componentManagerId").getString(),
						IGeni.GENI_ALLOCATED);
				return reservationDetails;
			}
		}
		return null;
	}

	public static void addToCreateRequest(
			final Map<String, ReservationDetails> reservations,
			final Model modelCreate) throws ResourceRepositoryException {

		for (Map.Entry<String, ReservationDetails> instance : reservations
				.entrySet()) {

			String componentManagerId = instance.getValue()
					.getComponentManangerId().toString();
			Resource resourceAdapter = modelCreate
					.createResource(componentManagerId);

			resourceAdapter.addProperty(RDF.type,
					getResourceAdapterName(componentManagerId));
			Resource resource = modelCreate.createResource(instance.getKey());
			resource.addProperty(RDF.type, getResourceName(componentManagerId));
		}
	}

	public static String getGroupURI(String reservation) throws ResourceRepositoryException{
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
	
	private static Resource getResourceAdapterName(Object componentManagerId)
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

	private static Resource getResourceName(Object componentManangerId)
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

	public static void updateReservations(Model model, Map<String, Group> groups)
			throws ResourceRepositoryException {

		StmtIterator iterator = model.listStatements();
		final List<String> reservationsURI = new LinkedList<>();

		while (iterator.hasNext()) {

			Resource provisionedInstance = iterator.next().getSubject();
			String instance = provisionedInstance.getURI();

			if (!reservationsURI.contains(instance)) {
				reservationsURI.add(instance);
			}
		}

		for (String reservationURI : reservationsURI) {

			for (Map.Entry<String, Group> group : groups.entrySet()) {

				if (group.getValue().getReservations().keySet()
						.contains(reservationURI)) {

					changeReservationState(model, reservationURI);
					group.getValue().getReservations().get(reservationURI)
							.setStatus(IGeni.GENI_PROVISIONED);
					break;
				}
			}
		}
	}

	private static void changeReservationState(Model model,
			String reservationURI) throws ResourceRepositoryException {

		Model deleteModel = ModelFactory.createDefaultModel();
		Resource deleteResource = deleteModel.createResource(reservationURI);
		TripletStoreAccessor.removePropertyValue(deleteResource,
				MessageBusOntologyModel.hasState);
		Resource provisionedState = model.createResource(reservationURI);
		provisionedState.addProperty(MessageBusOntologyModel.hasState,
				IGeni.GENI_PROVISIONED);
	}

	public static boolean allInstancesProvisioned(Map<String, Group> groups) {

		boolean provisionFinished = true;

		for (Map.Entry<String, Group> group : groups.entrySet()) {

			for (Map.Entry<String, ReservationDetails> reservation : group
					.getValue().getReservations().entrySet()) {

				if (!reservation.getValue().getStatus()
						.equals(IGeni.GENI_PROVISIONED)) {
					provisionFinished = false;
					break;
				}
			}
			if (!provisionFinished) {
				break;
			}
		}
		return provisionFinished;
	}

	public static Model createProvisionResponse(Set<String> groupsURN)
			throws ResourceRepositoryException {

		Model provisionResponseModel = ModelFactory.createDefaultModel();
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

			provisionResponseModel.add(QueryExecuter
					.executeSparqlDescribeQuery(reservationQuery));

		}

		return provisionResponseModel;
	}

}
