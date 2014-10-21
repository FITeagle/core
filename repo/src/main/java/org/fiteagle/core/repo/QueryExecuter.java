package org.fiteagle.core.repo;

import java.util.HashMap;
import java.util.Map;
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
  
  private static Map<String, String> missedNsPrefixes = new HashMap<>();
  
  static{
	  missedNsPrefixes.put("wgs", "http://www.w3.org/2003/01/geo/wgs84_pos#");
	  missedNsPrefixes.put("motor", "http://fiteagle.org/ontology/adapter/motor#");
	  missedNsPrefixes.put("omn","http://fiteagle.org/ontology#");
	  missedNsPrefixes.put("mightyrobot","http://fiteagle.org/ontology/adapter/mightyrobot#");
	  missedNsPrefixes.put("foaf","http://xmlns.com/foaf/0.1/");
	  missedNsPrefixes.put("dc","http://purl.org/dc/elements/1.1/");
	  missedNsPrefixes.put("","http://fiteagleinternal#"); 
  }
  
  
  public static ResultSet executeSparqlSelectQuery(String queryString) throws QueryParseException{
    ResultSet rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
    rs = qe.execSelect();
    return rs;
  }
  
  public static Model executeSparqlConstructQuery(String queryString) throws QueryParseException{
	    Model rs = null;
	    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
	    rs = qe.execConstruct();
	    
	    //todo: find a better way to set our own common known prefixes (e.g. omn, wgs, ...)
	    Map<String, String> nsPrefix = new HashMap<>();
	    nsPrefix = rs.getNsPrefixMap();
	    
	    for(Map.Entry<String, String> entry : nsPrefix.entrySet()){
	    	if (entry.getKey().toString().contains("j.")){
	    		for(Map.Entry<String, String> staticEntry : missedNsPrefixes.entrySet()){
	    			if(entry.getValue().toString().equals(staticEntry.getValue().toString())){
	    				rs.setNsPrefix(staticEntry.getKey().toString(), staticEntry.getValue().toString());
	    				rs.removeNsPrefix(entry.getKey());
	    			}
	    		}
	    	}
	    }
	    return rs;
	  }
  
  public static Model executeSparqlDescribeQuery(String queryString) throws QueryParseException{
    Model rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
    rs = qe.execDescribe();
    return rs;
  }
  
}
