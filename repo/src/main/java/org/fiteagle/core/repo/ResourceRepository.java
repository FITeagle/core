package org.fiteagle.core.repo;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fiteagle.api.core.IResourceRepository;

import com.hp.hpl.jena.rdf.model.Model;

public class ResourceRepository implements IResourceRepository {

	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());

	public String listResources(final String type) {
		final String inputFilename = "dummy-answer.xml";
		InputStream inputStream = this.getClass().getClassLoader()
		.getResourceAsStream(inputFilename);
		StringWriter result = new StringWriter();

		ResourceRepository.LOGGER.log(Level.INFO, "Dummy response from: "
				+ inputFilename + " to format: " + type);
		Model model = RDFDataMgr.loadModel(inputFilename, Lang.RDFXML);
		model.read(inputStream, IResourceRepository.SERIALIZATION_RDFXML_PLAIN);
		model.write(result, type);
		
		return result.toString();
	}
}
