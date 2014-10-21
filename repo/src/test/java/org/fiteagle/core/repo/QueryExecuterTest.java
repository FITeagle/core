package org.fiteagle.core.repo;

import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.rdf.model.Model;

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

}