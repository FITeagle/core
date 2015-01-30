package org.fiteagle.core.tripletStoreAccessor;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.Template;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import javax.xml.crypto.Data;

public class TripletStoreAccessor {
  
  private static Logger LOGGER = Logger.getLogger(TripletStoreAccessor.class.toString());

  private final static String FUNCTIONAL_PROPERTY = "http://www.w3.org/2002/07/owl#FunctionalProperty";

  public static String handleSPARQLRequest(String sparqlQuery, String serialization) throws ResourceRepositoryException, ParsingException {
    Model resultModel = null;
    String resultJSON = "";

    LOGGER.log(Level.INFO, "Processing SPARQL Query:\n" + sparqlQuery);

    if (sparqlQuery.toUpperCase().contains("SELECT")) {
      ResultSet rs = QueryExecuter.executeSparqlSelectQuery(sparqlQuery);
      resultJSON = MessageUtil.parseResultSetToJson(rs);
      resultModel = ResultSetFormatter.toModel(rs);
    } else if (sparqlQuery.toUpperCase().contains("DESCRIBE")) {
      resultModel = QueryExecuter.executeSparqlDescribeQuery(sparqlQuery);
      resultJSON = MessageUtil.serializeModel(resultModel, IMessageBus.SERIALIZATION_JSONLD);

    } else if (sparqlQuery.toUpperCase().contains("CONSTRUCT")) {
      resultModel = QueryExecuter.executeSparqlConstructQuery(sparqlQuery);
      resultJSON = MessageUtil.serializeModel(resultModel, IMessageBus.SERIALIZATION_JSONLD);
    }

    switch(serialization){
      case IMessageBus.SERIALIZATION_TURTLE:
        return MessageUtil.serializeModel(resultModel, IMessageBus.SERIALIZATION_TURTLE);
      case IMessageBus.SERIALIZATION_JSONLD:
        return resultJSON;
    }
    throw new ResourceRepositoryException("Unsupported serialization type: "+serialization);
  }

  public static void deleteModel(Model modelToDelete) throws ResourceRepositoryException {
    StmtIterator iterator = modelToDelete.listStatements();
    while(iterator.hasNext()){
      removeStatement(iterator.next());
    }
  }

  public static void deleteResource(Resource resourceToRemove) throws ResourceRepositoryException {
    String resource =  "<"+resourceToRemove.getURI()+"> ?anyPredicate ?anyObject .";

    String updateString = "DELETE { "+resource+" }" + "WHERE { "+resource+" }";

    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }

  public static void updateRepositoryModel(Model modelInform) throws ResourceRepositoryException {
    DatasetAccessor accessor = getTripletStoreAccessor();
    Model currentModel = accessor.getModel();
    Property functionalProperty = currentModel.getProperty(FUNCTIONAL_PROPERTY);
    StmtIterator iter = modelInform.listStatements();
    while(iter.hasNext()){
      Statement st = iter.next();
      if(currentModel.getProperty(st.getPredicate().getURI()).hasProperty(RDF.type, functionalProperty)){
        removePropertyValue(st.getSubject(), st.getPredicate());
      }
    }
    insertDataFromModel(modelInform);
  }

  private static void insertDataFromModel(Model model) throws ResourceRepositoryException{
    for(Entry<String, String> p : model.getNsPrefixMap().entrySet()){
      model.removeNsPrefix(p.getKey());
    }

    String updateString = "INSERT DATA { "+MessageUtil.serializeModel(model, IMessageBus.SERIALIZATION_TURTLE)+" }";

    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }

  public static void removeStatement(Statement statement) throws ResourceRepositoryException{
    String existingValue = "";
    if(statement.getObject().isResource()){
      existingValue = "<"+statement.getSubject().getURI()+"> <"+statement.getPredicate().getURI()+"> <"+statement.getResource().getURI()+"> .";
    }
    else{
      existingValue = "<"+statement.getSubject().getURI()+"> <"+statement.getPredicate().getURI()+"> "+statement.getLiteral()+" .";
    }

    String updateString = "DELETE { "+existingValue+" }" + "WHERE { "+existingValue+" }";

    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }

  public static void removePropertyValue(Resource subject, Resource predicate) throws ResourceRepositoryException{
    String existingValue = "<"+subject.getURI()+"> <"+predicate.getURI()+"> ?anyObject .";

    String updateString = "DELETE { "+existingValue+" }" + "WHERE { "+existingValue+" }";

    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }

  private static DatasetAccessor getTripletStoreAccessor() throws ResourceRepositoryException {
    DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(QueryExecuter.SESAME_SERVICE_DATA);
    if (accessor == null) {
      throw new ResourceRepositoryException("Could not connect to fuseki service at:" + QueryExecuter.SESAME_SERVICE_DATA);
    }
    return accessor;
  }

  public static Model getInfrastructure() throws ResourceRepositoryException {

      String queryString = "CONSTRUCT { ?infrastructure ?o ?p} WHERE { ?infrastructure " + RDF.type.getNameSpace() +RDF.type.getLocalName() + " " + Omn_federation.Infrastructure.getNameSpace() + Omn_federation.Infrastructure.getLocalName()  +"}" ;
      Query query = QueryFactory.create();
      query.setQueryConstructType();
      query.addResultVar("infrastructure");

      Triple tripleForPattern = new Triple(new Node_Variable("infrastructure"),new Node_Variable("o"),new Node_Variable("p"));
      BasicPattern constructPattern = new BasicPattern();
      constructPattern.add(tripleForPattern);
      query.setConstructTemplate(new Template(constructPattern));

      ElementGroup whereClause = new ElementGroup();
      whereClause.addTriplePattern(new Triple(new Node_Variable("infrastructure"), RDF.type.asNode(), Omn_federation.Infrastructure.asNode()));
      whereClause.addTriplePattern(tripleForPattern);
      query.setQueryPattern(whereClause);




        LOGGER.log(Level.INFO, query.serialize());
  //    Query query =  QueryFactory.create(queryString);
      QueryExecution queryExecution = QueryExecutionFactory.sparqlService(QueryExecuter.SESAME_SERVICE, query);



      Model model  = queryExecution.execConstruct();


    return model;
  }

    private void setConstructPattern() {

    }
  public static class ResourceRepositoryException extends Exception {

    private static final long serialVersionUID = 8213556984621316215L;

    public ResourceRepositoryException(String message){
      super(message);
    }
  }

  public static void addResource(Resource resource) throws ResourceRepositoryException {
    DatasetAccessor accessor = TripletStoreAccessor.getTripletStoreAccessor();
    accessor.add(resource.getModel());
  }
}
