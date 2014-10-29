package org.fiteagle.core.repo;

import org.fiteagle.api.core.MessageBusMsgFactory;
import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class QueryExecuterTest {

	@Test(expected = QueryParseException.class)
	public void testInvalidQuery() {
		QueryExecuter
				.executeSparqlDescribeQuery("This is no valid SPARQL Query");
	}

//	@Test
	public void test2() {
		Model rs = QueryExecuter
				.executeSparqlConstructQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
						+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
						+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
						+ "CONSTRUCT { ?testbed rdf:type omn:Testbed. ?testbed rdfs:label ?label. "
						+ "?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } "
						+ "FROM <http://localhost:3030/ds/query> "
						+ "WHERE {?testbed rdf:type omn:Testbed. "
						+ "OPTIONAL {?testbed rdfs:label ?label. ?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } }");
		System.out.println(rs.getGraph());
	}
	
//	 @Test
	  public void testListResources() {
		  String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "CONSTRUCT { ?resource omn:partOfGroup <http://federation.av.tu-berlin.de/about#AV_Smart_Communication_Testbed>."
					+ "?resource rdfs:label ?label. "
					+ "?resource rdfs:comment ?comment."
					+ "?resource rdf:type ?type. "
					+ "?resource wgs:lat ?lat. "
					+ "?resource wgs:long ?long. } "
					+ "FROM <http://localhost:3030/ds/query> "
					+ "WHERE {?resource omn:partOfGroup <http://federation.av.tu-berlin.de/about#AV_Smart_Communication_Testbed>. "
					+ "OPTIONAL {?resource rdfs:label ?label. "
					+ "?resource rdfs:comment ?comment. "
					+ "?resource rdf:type ?type. "
					+ "?resource wgs:lat ?lat. "
					+ "?resource wgs:long ?long. } }";
		  
		  
		  Model rs = QueryExecuter.executeSparqlConstructQuery(query);
		  
		  StmtIterator iter = rs.listStatements();
		  while(iter.hasNext()){
			  System.out.println(iter.next());
		  }
	  }
	  
	//	 @Test
		  public void testgetExtensions() {
			  String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
			          + "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
			          + "PREFIX av: <http://federation.av.tu-berlin.de/about#> "
			          + "CONSTRUCT { ?resource omn:partOfGroup av:AV_Smart_Communication_Testbed."
			          + "?resource wgs:lat ?lat. ?resource wgs:long ?long. ?resource <http://open-multinet.info/ontology/resource/>. "
			          + "} "
			          + "FROM <http://localhost:3030/ds/query> "
			          + "WHERE {?resource omn:partOfGroup av:AV_Smart_Communication_Testbed. "
			          + "OPTIONAL {?resource <http://open-multinet.info/ontology/resource/>. } "
			          + "}";
			  
			  
			  Model rs = QueryExecuter.executeSparqlConstructQuery(query);
			  
			  StmtIterator iter = rs.listStatements();
			  while(iter.hasNext()){
				  System.out.println(iter.next());
			  }
		  }
		  
//	     @Test
      public void testDescribe() {
       String query = "DESCRIBE ?property WHERE {?property <http://www.w3.org/2000/01/rdf-schema#domain>  <http://open-multinet.info/ontology/resource/motor#Motor> . }";
        
        Model rs = QueryExecuter.executeSparqlDescribeQuery(query);
        
        String result = MessageBusMsgFactory.serializeModel(rs);
        System.out.println(result);
      }

}

 