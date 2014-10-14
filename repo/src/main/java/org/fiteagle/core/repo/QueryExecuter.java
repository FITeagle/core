package org.fiteagle.core.repo;

import java.util.logging.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class QueryExecuter {
  
  private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/query";
  
  @SuppressWarnings("unused")
  private static Logger LOGGER = Logger.getLogger(QueryExecuter.class.toString());
  
  public static ResultSet executeSparqlSelectQuery(String queryString) throws QueryParseException{
    ResultSet rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
    rs = qe.execSelect();
    return rs;
  }
  
  public static Model executeSparqlDescribeQuery(String queryString) throws QueryParseException{
    Model rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
    rs = qe.execDescribe();
    return rs;
  }
  
}
