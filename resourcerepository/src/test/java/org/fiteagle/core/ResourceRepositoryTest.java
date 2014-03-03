package org.fiteagle.core;


import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
    @Test
    public void testListResources() {
    	IResourceRepository repo = new ResourceRepository();
    	String result = repo.listResources(Serialization.TTL);
    	Assert.assertTrue(result.contains("@prefix"));
    	result = repo.listResources(Serialization.XML);
    	Assert.assertTrue(result.contains("<rdf"));
    }
}
