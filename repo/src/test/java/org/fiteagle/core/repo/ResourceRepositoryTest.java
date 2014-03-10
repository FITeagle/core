package org.fiteagle.core.repo;

import org.fiteagle.api.core.IResourceRepository;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
	
	@Test
	public void testListResources() {
		final IResourceRepository repo = new ResourceRepository();
		String result = repo.listResources(IResourceRepository.SERIALIZATION_TURTLE);
		Assert.assertTrue(result.contains("@prefix"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
		Assert.assertTrue(result.contains("<rdf"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_RDFJSON);
		Assert.assertTrue(result.contains("value"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_JSONLD);
		Assert.assertTrue(result.contains("@id"));
	}	
}
