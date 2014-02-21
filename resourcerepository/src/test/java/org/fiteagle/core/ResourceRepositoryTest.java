package org.fiteagle.core;


import org.fiteagle.api.core.ResourceRepository;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
    @Test
    public void testListResources() {
    	ResourceRepository repo = new ResourceRepositoryEJB();
    	String result = repo.listResources();
    	Assert.assertTrue(result.contains("rdf"));
    }
}
