package org.fiteagle.regionManagement.dao;

import java.util.ArrayList;
import java.util.Calendar;
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

import org.fiteagle.api.ContactInformation;
import org.fiteagle.api.IEndpointDAO;
import org.fiteagle.api.IRegionDAO;
import org.fiteagle.api.Region;
import org.fiteagle.api.RegionStatus;

@Stateless(mappedName = "IRegionDAO")
@Remote(IRegionDAO.class)
public class RegionDAO implements IRegionDAO {

	@PersistenceContext(unitName = "registryDB")
	EntityManager em;

	@EJB
	IEndpointDAO endpointDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fiteagle.xifi.api.dao.IRegionDAO#createRegion(org.fiteagle.xifi.api
	 * .model.Region)
	 */
	@Override
	public Region createRegion(Region region) {

		RegionStatus status = new RegionStatus();

		em.persist(region);
		status.setRegion(region.getId());
		status.setTimestamp(Calendar.getInstance().getTimeInMillis());
		status.setStatus("created");
		region.setRegionStatus(status);
		em.merge(region);
		return region;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#findRegion(long)
	 */
	@Override
	public Region findRegion(long regionid) {
		Region r = em.find(Region.class, regionid);
		return r;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#findRegions(java.lang.String)
	 */
	@Override
	public List<? extends Region> findRegions(String country) {
		CriteriaBuilder ctb = em.getCriteriaBuilder();
		CriteriaQuery<Region> query = ctb.createQuery(Region.class);
		Root<Region> root = query.from(Region.class);
		query.select(root);
		List<Predicate> predicateList = new ArrayList<>();
		Predicate countryPred;
		if (country != null) {
			countryPred = ctb.equal(root.get("country"), country);
			if (countryPred != null) {
				predicateList.add(countryPred);
			}
		}
		if (predicateList.size() > 0) {
			Predicate[] predicates = new Predicate[predicateList.size()];
			predicateList.toArray(predicates);
			query.where(predicates);
		}
		return em.createQuery(query).getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fiteagle.xifi.api.dao.IRegionDAO#updateRegion(org.fiteagle.api.IRegion
	 * )
	 */
	@Override
	public Region updateRegion(Region r) {
		Region former = em.find(Region.class, r.getId());
		if (former != null) {
			if (r.getAdminUsername() != null) {
				former.setAdminUsername(r.getAdminUsername());
			}
			if (r.getCountry() != null) {
				former.setCountry(r.getCountry());
			}
			if (r.getLatitude() != null) {
				former.setLatitude(r.getLatitude());
			}
			if (r.getLongitude() != null) {
				former.setLongitude(r.getLongitude());
			}
			if (r.getNodeType() != null) {
				former.setNodeType(r.getNodeType());
			}

			em.merge(former);
		}
		return former;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#deleteRegion(long)
	 */
	@Override
	public void deleteRegion(long regionid) {
		Region r = em.getReference(Region.class, regionid);
		if (r != null)
			try {
				em.remove(r);
				endpointDao.deleteEndpointsForRegion(regionid);
			} catch (EntityNotFoundException e) {

			}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#findRegionStatusForId(long)
	 */
	@Override
	public RegionStatus findRegionStatusForId(long regionid) {
		RegionStatus regionStatus = em.find(RegionStatus.class, regionid);
		return regionStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fiteagle.xifi.api.dao.IRegionDAO#updateRegionStatus(org.fiteagle.
	 * xifi.api.model.RegionStatus)
	 */
	@Override
	public RegionStatus updateRegionStatus(RegionStatus status) {
		Region r = em.find(Region.class, status.getRegion());
		RegionStatus updated = null;
		if (r != null) {

			updated = em.merge(status);
		}
		return updated;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#addContactInforamtion(long,
	 * org.fiteagle.xifi.api.model.ContactInformation)
	 */
	@Override
	public ContactInformation addContactInforamtion(long regionid,
			ContactInformation contactInfo) {
		Region r = em.find(Region.class, regionid);
		r.addContact(contactInfo);
		ContactInformation created = null;
		em.merge(r);
		List<ContactInformation> contacts = r.getContacts();
		for (ContactInformation c : contacts) {
			if (c.equals(contactInfo))
				created = c;
		}
		return created;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#getContacts(long,
	 * java.lang.String)
	 */
	@Override
	public List<? extends ContactInformation> getContacts(long regionid,
			String type) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

		CriteriaQuery<ContactInformation> query = criteriaBuilder
				.createQuery(ContactInformation.class);
		Root<ContactInformation> root = query.from(ContactInformation.class);
		query.select(root);
		List<Predicate> predicateList = new ArrayList<>();
		Predicate regionidpred, typepred;
		regionidpred = criteriaBuilder.equal(root.get("region").get("id"),
				regionid);
		predicateList.add(regionidpred);
		if (type != null) {
			typepred = criteriaBuilder.equal(root.get("type"), type);
			if (typepred != null) {
				predicateList.add(typepred);
			}
		}
		if (predicateList.size() > 0) {
			Predicate[] predicates = new Predicate[predicateList.size()];
			predicateList.toArray(predicates);
			query.where(predicates);
		}
		return em.createQuery(query).getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#getContactInfo(long)
	 */
	@Override
	public ContactInformation getContactInfo(long contactId) {
		return em.find(ContactInformation.class, contactId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#updateContactInformation(long,
	 * org.fiteagle.api.IContactInformation)
	 */
	@Override
	public ContactInformation updateContactInformation(long contactId,
			ContactInformation updated) {
		ContactInformation former = em
				.find(ContactInformation.class, contactId);
		if (updated.getAddress() != null) {
			former.setAddress(updated.getAddress());
		}
		if (updated.getCountry() != null) {
			former.setCountry(updated.getCountry());
		}
		if (updated.getEmail() != null) {
			former.setEmail(updated.getEmail());
		}
		if (updated.getFax() != null) {
			former.setFax(updated.getFax());
		}
		if (updated.getName() != null) {
			former.setName(updated.getName());
		}
		if (updated.getPhone() != null) {
			former.setPhone(updated.getPhone());
		}
		if (updated.getType() != null) {
			former.setType(updated.getType());
		}

		em.merge(former);
		return former;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fiteagle.xifi.api.dao.IRegionDAO#deleteContact(long)
	 */
	@Override
	public void deleteContact(long contactId) {
//		ContactInformation c = em.getReference(ContactInformation.class,
//				contactId);
		
		ContactInformation c = em.find(ContactInformation.class, contactId);
		c.setRegion(null);
		em.remove(c);

	}

}
