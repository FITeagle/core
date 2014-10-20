package org.fiteagle.core.repo;

import java.util.logging.Logger;

import org.fiteagle.api.core.MessageBusMsgFactory;

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
    
    //todo: find a better way to set our own common known prefixes (e.g. omn, wgs, ...)
    rs.removeNsPrefix("j.0");
    rs.removeNsPrefix("j.1");
    rs.removeNsPrefix("j.2");
    rs.setNsPrefix("omn", "http://fiteagle.org/ontology#");
    rs.setNsPrefix("wgs", "http://www.w3.org/2003/01/geo/wgs84_pos#");
    rs.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");

    //MessageBusMsgFactory.setCommonPrefixes(rs);
    return rs;
  }
  
}
