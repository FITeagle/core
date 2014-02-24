package org.fiteagle.regionManagement.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.fiteagle.api.IEndpointDAO;
import org.fiteagle.api.IService;
import org.fiteagle.api.IServiceDAO;
import org.fiteagle.regionManagement.dao.model.Service;

@Stateless(name="ServiceDAO", mappedName="IServiceDAO")
@Remote(IServiceDAO.class)
public class ServiceDAO implements IServiceDAO {

	@PersistenceContext(unitName="registryDB")
	EntityManager em;
	@EJB
	IEndpointDAO endpointDAO;
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.IServiceDAO#createService(org.fiteagle.api.IService)
	 */
	@Override
	public IService createService(IService service){
		em.persist(service);
		return service;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.IServiceDAO#findServices(java.lang.String)
	 */
	@Override
	public List<? extends IService> findServices(String type) {
		CriteriaBuilder ctb = em.getCriteriaBuilder();
		CriteriaQuery<Service> query = ctb.createQuery(Service.class);
		Root<Service> root = query.from(Service.class);
		query.select(root);
		List<Predicate> predicateList = new ArrayList<>();
		Predicate typePred;
		if(type != null){
			typePred = ctb.equal(root.get("type"), type);
			if(typePred!= null){
				predicateList.add(typePred);
			}
		}
		if(predicateList.size()>0){
			Predicate[] predicates = new Predicate[predicateList.size()];
			predicateList.toArray(predicates);
			query.where(predicates);
		}
		return em.createQuery(query).getResultList();
	}



	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.IServiceDAO#findService(long)
	 */
	@Override
	public IService findService(long serviceid) {
		return em.find(Service.class, serviceid);
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.IServiceDAO#updateService(org.fiteagle.xifi.api.model.Service)
	 */
	@Override
	public IService updateService(IService service) {
		return em.merge(service);
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.IServiceDAO#deleteService(long)
	 */
	@Override
	public void deleteService(long serviceid) {
		IService s = em.getReference(Service.class, serviceid);
		if(s!= null){
			try{
				em.remove(s);
				endpointDAO.deleteEndpointForServiceId(serviceid);
			}catch(EntityNotFoundException e){
				
			}
		}
		
		
	}
}
