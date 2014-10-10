package org.fiteagle.core.repo;

import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;

public class QueryExecuterTest {

  @Test(expected=QueryParseException.class)
	public void testInvalidQuery(){
		QueryExecuter.queryModelFromDatabase("This is no valid SPARQL Query");
	}
  
}
