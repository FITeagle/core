package org.fiteagle.core.repo;

import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class QueryExecuterTest {

	//@Test(expected = QueryParseException.class)
	public void testInvalidQuery() {
		QueryExecuter
				.executeSparqlDescribeQuery("This is no valid SPARQL Query");
	}

//	@Test
	public void test2() {
		Model rs = QueryExecuter
				.executeSparqlConstructQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ "PREFIX omn: <http://fiteagle.org/ontology#> "
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
					+ "PREFIX omn: <http://open-multinet.info/ontology#> "
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

}

 