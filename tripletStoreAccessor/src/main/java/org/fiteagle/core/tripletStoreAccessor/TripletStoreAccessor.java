package org.fiteagle.core.tripletStoreAccessor;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class TripletStoreAccessor {
  
  private static Logger LOGGER = Logger.getLogger(TripletStoreAccessor.class.toString());
  
  public static String handleSPARQLRequest(Model requestModel, String serialization) throws ResourceRepositoryException, ParsingException {
    String sparqlQuery = MessageUtil.getSPARQLQueryFromModel(requestModel);
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
  
  public static void releaseResource(Resource resourceToRemove) throws ResourceRepositoryException {
    String resource =  "<"+resourceToRemove.getURI()+"> ?anyPredicate ?anyObject .";
    
    String updateString = "DELETE { "+resource+" }" + "WHERE { "+resource+" }";
    
    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }
  
  public static void updateRepositoryModel(Model modelInform) throws ResourceRepositoryException {
    StmtIterator iter = modelInform.listStatements();
    while(iter.hasNext()){
      removeExistingValue(iter.next());
    }
    insertDataFromModel(modelInform);    
  }
  
  private static void insertDataFromModel(Model model) throws ResourceRepositoryException{
    for(Entry<String, String> p : model.getNsPrefixMap().entrySet()){
      model.removeNsPrefix(p.getKey());
    }
    
    String updateString = "INSERT DATA { "+MessageUtil.serializeModel(model)+" }";
    
    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }
  
  private static void removeExistingValue(Statement statement) throws ResourceRepositoryException{
    String existingValue = "<"+statement.getSubject().getURI()+"> <"+statement.getPredicate().getURI()+"> ?anyObject .";
          
    String updateString = "DELETE { "+existingValue+" }" + "WHERE { "+existingValue+" }";

    QueryExecuter.executeSparqlUpdateQuery(updateString);
  }
  
  public static class ResourceRepositoryException extends Exception {
    
    private static final long serialVersionUID = 8213556984621316215L;

    public ResourceRepositoryException(String message){
      super(message);
    }
  }
  
}
