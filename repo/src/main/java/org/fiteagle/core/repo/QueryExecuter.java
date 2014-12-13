package org.fiteagle.core.repo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class QueryExecuter {
  
  @SuppressWarnings("unused")
  private static Logger LOGGER = Logger.getLogger(QueryExecuter.class.toString());
  
  private static Map<String, String> missedNsPrefixes = new HashMap<>();
  
  static{
	  missedNsPrefixes.put("wgs", "http://www.w3.org/2003/01/geo/wgs84_pos#");
	  missedNsPrefixes.put("omn","http://open-multinet.info/ontology/omn#");
	  missedNsPrefixes.put("av","http://federation.av.tu-berlin.de/about#");
	  missedNsPrefixes.put("foaf","http://xmlns.com/foaf/0.1/");
	  missedNsPrefixes.put("dc","http://purl.org/dc/elements/1.1/");
	  missedNsPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	  missedNsPrefixes.put("motorgarage","http://open-multinet.info/ontology/resource/motorgarage#");
	  missedNsPrefixes.put("motor","http://open-multinet.info/ontology/resource/motor#");
  }
  
  public static ResultSet executeSparqlSelectQuery(String queryString) throws QueryParseException{
    ResultSet rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(ResourceRepoHandler.FUSEKI_SERVICE_QUERY, queryString);
    rs = qe.execSelect();
    return rs;
  }
  
  public static Model executeSparqlConstructQuery(String queryString) throws QueryParseException{
	    Model rs = null;
	    QueryExecution qe = QueryExecutionFactory.sparqlService(ResourceRepoHandler.FUSEKI_SERVICE_QUERY, queryString);
	    rs = qe.execConstruct();
	    correctNsPrefixes(rs);

	    return rs;
	  }
  
  public static void correctNsPrefixes(Model model){
	    //todo: find a better way to set our own common known prefixes (e.g. omn, wgs, ...)
	    Map<String, String> nsPrefix = model.getNsPrefixMap();
	    for(Map.Entry<String, String> entry : nsPrefix.entrySet()){
	      String currentKey = entry.getKey();
	    	if(currentKey.toString().startsWith("j.")){
	    	  model.removeNsPrefix(currentKey);
	    		for(Map.Entry<String, String> staticEntry : missedNsPrefixes.entrySet()){
	    			if(currentKey.toString().equals(staticEntry.getValue().toString())){
	    				model.setNsPrefix(staticEntry.getKey().toString(), staticEntry.getValue().toString());
	    			}
	    		}
	    	}
	    }
  }
  
  public static void removeProblematicNsPrefixes(Model model){
    Map<String, String> nsPrefix = model.getNsPrefixMap();
    for(Map.Entry<String, String> entry : nsPrefix.entrySet()){
      String currentKey = entry.getKey();
      if(currentKey.toString().startsWith("j.")){
        model.removeNsPrefix(currentKey);
      }
    }
  }
  
  public static Model executeSparqlDescribeQuery(String queryString) throws QueryParseException{
    Model rs = null;
    QueryExecution qe = QueryExecutionFactory.sparqlService(ResourceRepoHandler.FUSEKI_SERVICE_QUERY, queryString);
    rs = qe.execDescribe();
    correctNsPrefixes(rs);
    return rs;
  }
  
}
