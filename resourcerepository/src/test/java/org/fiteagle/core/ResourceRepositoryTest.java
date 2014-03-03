package org.fiteagle.core;


import org.fiteagle.api.core.IResourceRepository;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
    @Test
    public void testListResources() {
    	IResourceRepository repo = new ResourceRepository();
    	String result = repo.listResources();
    	Assert.assertTrue(result.contains("rdf"));
    }
}
