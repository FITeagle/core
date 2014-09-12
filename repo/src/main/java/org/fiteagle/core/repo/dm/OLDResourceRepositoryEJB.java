package org.fiteagle.core.repo.dm;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.OLDResourceRepository;

@Stateless
@Remote(IResourceRepository.class)
public class OLDResourceRepositoryEJB implements IResourceRepository {
	private final OLDResourceRepository repo;

	public OLDResourceRepositoryEJB() {
		this.repo = new OLDResourceRepository();
	}

	public String listResources(final String type) {
		return this.repo.listResources(type);
	}

	public String listResources() {
		return this.repo.listResources();
	}

	public String queryDatabse(String query, String serialization) {
		return this.repo.queryDatabse(query, serialization);
	}
}
