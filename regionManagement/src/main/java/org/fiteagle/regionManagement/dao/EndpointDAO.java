package org.fiteagle.regionManagement.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.fiteagle.api.Endpoint;
import org.fiteagle.api.IEndpointDAO;

@Stateless(name = "EndpointDAO", mappedName="IEndpointDAO")
@Remote(EndpointDAO.class)
public class EndpointDAO implements IEndpointDAO {
	@PersistenceContext(unitName="registryDB")
	EntityManager em;

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#findEndpoints(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<? extends Endpoint> findEndpoints(String serviceId, String regionId,
			String interfaceType) {
		
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Endpoint> query = criteriaBuilder.createQuery(Endpoint.class);
			Root<Endpoint> root = query.from(Endpoint.class);
			query.select(root);
			List<Predicate> predicateList = new ArrayList<>();
			Predicate serviceidpred, regionidpred, interfaceTypepred;
			
			if(serviceId != null){
				serviceidpred = criteriaBuilder.equal(root.get("service_id"), Long.valueOf(serviceId));
				if(serviceidpred != null){
					predicateList.add(serviceidpred);
				}
					
			}
			
			if(regionId != null){
				regionidpred = criteriaBuilder.equal(root.get("regionId"), Long.valueOf(regionId));
				if(regionidpred != null){
					predicateList.add(regionidpred);
				}
			}
			
			if(interfaceType != null){
				interfaceTypepred = criteriaBuilder.equal(root.get("interfaceType"), interfaceType);
				if(interfaceTypepred != null){
					predicateList.add(interfaceTypepred);
				}
			}
			
			if(predicateList.size()>0){
				Predicate[] predicates = new Predicate[predicateList.size()];
				predicateList.toArray(predicates);
				query.where(predicates);
			}
			List<Endpoint> list =  em.createQuery(query).getResultList();
			return list;
			
			
		
			
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#addEndpoint(org.fiteagle.xifi.api.model.Endpoint)
	 */
	@Override
	public Endpoint addEndpoint(Endpoint endpoint) {
		 em.persist(endpoint);
		 return endpoint;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#findEndpoint(long)
	 */
	@Override
	public Endpoint findEndpoint(long endpointId) {
		Endpoint endpoint = em.find(Endpoint.class, endpointId);
		return endpoint;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#updateEndpoint(long, org.fiteagle.xifi.api.model.Endpoint)
	 */
	@Override
	public Endpoint updateEndpoint(long endpointId, Endpoint endpoint) {
		Endpoint from =  em.find(Endpoint.class, endpointId);
		if(endpoint.getInterfaceType() != null)
			from.setInterfaceType(endpoint.getInterfaceType());
		if(endpoint.getName()!= null)
			from.setName(endpoint.getName());
		if(endpoint.getRegionId() != 0)
			from.setRegionId(endpoint.getRegionId());
		if(endpoint.getService_id() != 0)
			from.setService_id(endpoint.getService_id());
		if(endpoint.getUrl() != null)
			from.setUrl(endpoint.getUrl());
		
		Endpoint e = em.merge(from);
		return e;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#deleteEndpoint(long)
	 */
	@Override
	public void deleteEndpoint(long endpointId) {
		Endpoint e = findEndpoint(endpointId);
		em.remove(e);
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#deleteEndpointsForRegion(long)
	 */
	@Override
	public void deleteEndpointsForRegion(long regionid) {
		CriteriaBuilder ctb  = em.getCriteriaBuilder();
		CriteriaDelete<Endpoint> query = ctb.createCriteriaDelete(Endpoint.class);
		
		Root<Endpoint> root =  query.from(Endpoint.class);
		Predicate regionpred = ctb.equal(root.get("regionId"),regionid);
		Predicate[] preds = new Predicate[]{regionpred};
		query.where(preds);
		em.createQuery(query).executeUpdate();
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.dao.EndpointDAO#deleteEndpointForServiceId(long)
	 */
	@Override
	public void deleteEndpointForServiceId(long serviceid) {
		CriteriaBuilder ctb  = em.getCriteriaBuilder();
		CriteriaDelete<Endpoint> query = ctb.createCriteriaDelete(Endpoint.class);
		
		Root<Endpoint> root =  query.from(Endpoint.class);
		Predicate servicepred = ctb.equal(root.get("service_id"),serviceid);
		Predicate[] preds = new Predicate[]{servicepred};
		query.where(preds);
		em.createQuery(query).executeUpdate();
		
	}

	
//	public void deleteEndpointForRegion(long regionId){
//		
//	}
	

}
