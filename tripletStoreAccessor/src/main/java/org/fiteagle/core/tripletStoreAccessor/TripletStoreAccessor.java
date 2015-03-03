package org.fiteagle.core.tripletStoreAccessor;

import info.openmultinet.ontology.vocabulary.Omn_federation;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;

import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteWhere;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.Template;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

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
      Model model = executeConstruct(query);


    return model;
  }

    private static Model executeConstruct(Query query) {
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(QueryExecuter.SESAME_SERVICE, query);


        return queryExecution.execConstruct();
    }


    public static String getResources() {

        Query query = QueryFactory.create();
        query.setQueryDescribeType();
        query.addResultVar("resource");
        Triple tripleForPattern = new Triple(new Node_Variable("resource"),new Node_Variable("o"),new Node_Variable("p"));


        ElementGroup whereClause = new ElementGroup();
        whereClause.addTriplePattern(new Triple(new Node_Variable("resource"), Omn_lifecycle.parentTo.asNode(), new Node_Variable("p")));
        whereClause.addTriplePattern(tripleForPattern);
        query.setQueryPattern(whereClause);

        QueryExecution execution =  QueryExecutionFactory.sparqlService(QueryExecuter.SESAME_SERVICE, query);
        Model model = execution.execDescribe();
        String serializedAnswer = MessageUtil.serializeModel(model,IMessageBus.SERIALIZATION_TURTLE);
        return serializedAnswer;
    }

    public static boolean exists(String uri) {
        Query query  = QueryFactory.create();
        query.setQueryAskType();
        Triple triple = new Triple(ResourceFactory.createResource(uri).asNode(), new Node_Variable("p"), new Node_Variable("o"));
        ElementGroup whereclause = new ElementGroup();
        whereclause.addTriplePattern(triple);
        query.setQueryPattern(whereclause);
        QueryExecution execution  = QueryExecutionFactory.sparqlService(QueryExecuter.SESAME_SERVICE, query);
        return execution.execAsk();

    }

    public static Model getResource(String uri) {
        Query query =  QueryFactory.create();
        query.setQueryDescribeType();
        query.addDescribeNode(ResourceFactory.createResource(uri).asNode());


        LOGGER.log(Level.INFO, query.serialize());
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(QueryExecuter.SESAME_SERVICE, query);
        Model model = queryExecution.execDescribe();
        return model;
    }

    public static void addModel(Model model) throws ResourceRepositoryException {
      DatasetAccessor datasetAccessor = getTripletStoreAccessor();
      try {
        datasetAccessor.add(model);
      } catch (HttpException e) {
        throw new ResourceRepositoryException(e.getMessage());
      }
    }

    public static void deleteModel(Model model) throws ResourceRepositoryException {

        StmtIterator stmtIterator = model.listStatements();
        List<Quad> quadList = new LinkedList<>();
        while (stmtIterator.hasNext()){
            Statement statement = stmtIterator.nextStatement();
            Quad quad = new Quad(Quad.defaultGraphIRI,statement.asTriple());
            quadList.add(quad);
        }
        QuadAcc quadAcc = new QuadAcc(quadList);
        UpdateDeleteWhere updateDeleteInsert = new UpdateDeleteWhere(quadAcc);
        UpdateRequest request = new UpdateRequest(updateDeleteInsert);
        UpdateProcessor qexec=UpdateExecutionFactory.createRemoteForm(request,QueryExecuter.SESAME_SERVICE_DATA);
        qexec.execute();
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

    public static Model getGraph() throws ResourceRepositoryException {
        DatasetAccessor accessor = TripletStoreAccessor.getTripletStoreAccessor();
        return accessor.getModel();
    }
}
