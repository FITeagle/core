package org.fiteagle.core.repo;

import java.util.logging.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

public class QueryExecuter {
  
  private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/query";
  
  @SuppressWarnings("unused")
  private static Logger LOGGER = Logger.getLogger(QueryExecuter.class.toString());
  
  /**
   * Submits a given sparql query string to the db
   * 
   * @param sparqlQuery
   *          String containing the sparql query to be issued
   * @return ResultSet retrieved from db, null on failure
   */
  public static ResultSet queryModelFromDatabase(String sparqlQuery) {
    ResultSet queryResult = submitSparqlQuery(sparqlQuery);
    return queryResult;
  }
  
  /**
   * Submits a given sparql query string to the db
   * 
   * @param queryString
   *          String containing the sparql query to be issued
   * @return ResultSet retrieved from db, null on failure
   */
  private static ResultSet submitSparqlQuery(String queryString) {
    ResultSet rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
    rs = qe.execSelect();
    return rs;
  }
  
}
