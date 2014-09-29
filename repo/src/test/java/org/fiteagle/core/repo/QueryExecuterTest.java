package org.fiteagle.core.repo;

import org.junit.Assert;
import org.junit.Test;

public class QueryExecuterTest {

	/**
	 * Test Querying SPARQL from the endpoint
	 */
	@Test
	public void testInvalidQuery(){
		// Submitting invalid SPARQL Query should return null, whether Triplet store is running or not
		Assert.assertNull(QueryExecuter.queryModelFromDatabase("This is no valid SPARQL Query"));
	}
}
