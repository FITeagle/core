package org.fiteagle.core.repo;

import org.junit.Assert;
import org.junit.Test;

public class QueryExecuterTest {

	@Test
	public void testInvalidQuery(){
		Assert.assertNull(QueryExecuter.queryModelFromDatabase("This is no valid SPARQL Query"));
	}
	
}
