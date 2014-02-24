package org.fiteagle.core;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.fiteagle.api.core.ResourceRepository;

@Stateless
@Remote(ResourceRepository.class)
public class ResourceRepositoryEJB implements ResourceRepository {

	public String listResources() {
		return "<rdf></rdf>";
	}
}
