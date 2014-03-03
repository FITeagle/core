package org.fiteagle.core;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.fiteagle.api.core.IResourceRepository;

@Stateless
@Remote(IResourceRepository.class)
public class ResourceRepository implements IResourceRepository {
	
	
	
	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());

	public String listResources(Serialization type) {
		String filename;
		if (Serialization.XML.equals(type)) {
			filename = "dummy-answer.xml";
		} else {
			filename = "dummy-answer.ttl";
		}
		
		LOGGER.log(Level.INFO, "Dummy response from: " + filename);
		return getContent(filename);
	}

	private String getContent(final String filename) {
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(filename);
		return convertStreamToString(is);
	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
