package org.fiteagle.core.orchestrator.provision;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.core.tripletStoreAccessor.QueryExecuter;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.jboss.resteasy.logging.Logger;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class HandleProvision {
  
  public static final  Map<String, Object> getReservations(String group) throws ResourceRepositoryException{
    final Map<String, Object> reservations = new HashMap<>();
    
    String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
        + "SELECT ?reservationId ?componentManagerId WHERE { "
        + "<" + group + "> a omn:Group ." 
        + "?reservationId omn:partOf \"" + group +"\" . "
        + "?reservationId omn:reserveInstanceFrom ?componentManagerId "
        + "}";
    ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(query);
    
    while(rs.hasNext()){
      QuerySolution qs = rs.next();
      if (qs.contains("reservationId") && qs.contains("componentManagerId")) {
        System.out.println("a sliver is found");
        System.out.println("reservation " + qs.getResource("reservationId").getURI() + " componentManagerId " + qs.getLiteral("componentManagerId").getString());
         reservations.put(qs.getResource("reservationId").getURI(), qs.getLiteral("componentManagerId").getString());
      }
    }
    System.out.println("The reservations map contains");
    for (Map.Entry<String, Object> instance : reservations.entrySet()) {
      System.out.println(instance.getKey() + " " + instance.getValue());
    }
    return reservations;
  }
  
  public static Model createRequest(final Map<String, Object> reservations) throws ResourceRepositoryException{
    Model createModel = ModelFactory.createDefaultModel();
    for (Map.Entry<String, Object> instance : reservations.entrySet()) {
      Resource resourceAdapter = createModel.createResource(instance.getValue().toString());
      resourceAdapter.addProperty(RDF.type, getResourceAdapterName(instance.getValue()));
      Resource resource = createModel.createResource(instance.getKey());
      resource.addProperty(RDF.type, getResourceName(instance.getValue()));
    }
    return createModel;
  }

  private static Resource getResourceAdapterName(Object componentManagerId) throws ResourceRepositoryException{
    String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
        + "SELECT ?resourceAdapter WHERE { "
        + "<" + componentManagerId + "> a ?resourceAdapter ."
        + "?resourceName omn:implementedBy ?resourceAdapter" 
        + "}";
    ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(query);
    Resource resourceName = null;
    while(rs.hasNext()){
      QuerySolution qs = rs.next();
      resourceName = qs.getResource("resourceAdapter");
    }
    return resourceName;
  }
  
  private static Resource getResourceName (Object componentManangerId) throws ResourceRepositoryException{
    String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
        + "SELECT ?resourceName WHERE { "
        + "<" + componentManangerId + "> a ?class ." 
        + "?resourceName omn:implementedBy ?class "
        + "}";
    ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(query);
    Resource resourceName = null;
    while(rs.hasNext()){
      QuerySolution qs = rs.next();
      resourceName = qs.getResource("resourceName");
    }
    return resourceName;
  }
  
  public static void reservationToRemove(Model model, Map<String, Object> reservationsMap) throws ResourceRepositoryException{
    StmtIterator iterator = model.listStatements();
    while(iterator.hasNext()){
      Resource instance = iterator.next().getSubject();
      if(instance.hasProperty(RDF.type)){
        Model modelToDelete = ModelFactory.createDefaultModel();
        Resource resource = modelToDelete.createResource(instance.getURI());
        resource.addProperty(RDF.type, MessageBusOntologyModel.classReservation);
        resource.addProperty(MessageBusOntologyModel.reserveInstanceFrom, reservationsMap.get(instance.getURI()).toString());
        TripletStoreAccessor.deleteRDFgraph(modelToDelete);
      }
    }
  }
}
