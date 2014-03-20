package org.fiteagle.core.repo;

import java.io.StringReader;

import org.fiteagle.api.core.IResourceRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
	
	private static final String DUMMY_DATA = "dummy-answer.xml";
	IResourceRepository repo;
	
	@Before
	public void setup() {
		this.repo = new ResourceRepository(DUMMY_DATA);
	}
	
	@Test
	public void testListResources() {
		
		String result;
		
		result = repo.listResources();
		Assert.assertTrue(result.contains("<rdf"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
		Assert.assertTrue(result.contains("<rdf"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_TURTLE);
		Assert.assertTrue(result.contains("<http"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_RDFJSON);
		Assert.assertTrue(result.contains("value"));
		result = repo.listResources(IResourceRepository.SERIALIZATION_JSONLD);
		Assert.assertTrue(result.contains("@id"));
	}	
	
	@Test
	public void testQuery() {
		String type = IResourceRepository.SERIALIZATION_JSONLD;
		String query = "SELECT ?resource WHERE {?resource <http://fiteagle.org/ontology#isInstantiatedBy> ?y}";
		String result = repo.queryDatabse(query, type);
		String expected = "http://fiteagle.org/resource#vm332";
		
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader(result), null, type);
		ResultSet rs = ResultSetFactory.fromRDF(model);
		while (rs.hasNext()) {
			QuerySolution solution = rs.next();
			String actual = solution.get("resource").toString();
			Assert.assertEquals(expected, actual);
		}
	}
}
