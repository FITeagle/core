package org.fiteagle.core.repo.dm;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceRepository;

@Stateless
@Remote(IResourceRepository.class)
public class ResourceRepositoryEJB implements IResourceRepository {
	private final ResourceRepository repo;

	public ResourceRepositoryEJB() {
		this.repo = new ResourceRepository();
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

  @Override
  public String listResources(String query, String type) {
    // TODO Auto-generated method stub
    return null;
  }
}
