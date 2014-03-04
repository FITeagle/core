package org.fiteagle.core.repository;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fiteagle.api.core.IResourceRepository;

public class ResourceRepository implements IResourceRepository {

	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());

	public String listResources(final Serialization type) {
		String filename;
		if (Serialization.XML.equals(type)) {
			filename = "dummy-answer.xml";
		} else {
			filename = "dummy-answer.ttl";
		}

		ResourceRepository.LOGGER.log(Level.INFO, "Dummy response from: "
				+ filename);
		return this.getContent(filename);
	}

	private String getContent(final String filename) {
		final InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(filename);
		return ResourceRepository.convertStreamToString(is);
	}

	static String convertStreamToString(final java.io.InputStream is) {
		final java.util.Scanner s = new java.util.Scanner(is)
				.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
}
