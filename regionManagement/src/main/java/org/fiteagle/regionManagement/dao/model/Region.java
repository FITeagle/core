package org.fiteagle.regionManagement.dao.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.fiteagle.api.IContactInformation;
import org.fiteagle.api.ILinkInfo;
import org.fiteagle.api.IRegion;
import org.fiteagle.api.IRegionStatus;


//@XmlRootElement
@Entity
public class Region extends LinkableEntity implements IRegion{
	
	@Id
	@Column(name="ID")
	@GeneratedValue
	long id;
	String country;
	String latitude;
	String longitude;
	String adminUsername;
	@OneToOne(cascade=CascadeType.ALL, targetEntity=RegionStatus.class)
	@PrimaryKeyJoinColumn
	IRegionStatus regionStatus;
	String nodeType;

	@Transient
	Map<String, List<ILinkInfo>> _links;
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#get_links()
	 */
	@Override
	public Map<String,List<ILinkInfo>> get_links() {
		return _links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#set_links(java.util.Map)
	 */
	@Override
	public void set_links(Map<String, List<ILinkInfo>> links) {
		this._links = links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#addLink(java.lang.String, org.fiteagle.xifi.api.model.LinkInfo)
	 */
	@Override
	public void addLink(String name,ILinkInfo l){
		List<ILinkInfo> list;
		if(this._links == null){
			_links = new HashMap<>();
			list = new ArrayList<>();
			list.add(l);
		}else{
			list = _links.get(name);
			if(list == null)
				list = new ArrayList<>();
			list.add(l);
		}
		_links.put(name,list);
	}
	
	
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER,targetEntity=ContactInformation.class)
	@JoinColumn(name="region_id", referencedColumnName="ID")
	private List<IContactInformation> contacts;
	

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getContacts()
	 */
	@Override
	@XmlTransient
	public List<IContactInformation> getContacts() {
		return contacts;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setContacts(java.util.List)
	 */
	@Override
	public void setContacts(List<IContactInformation> contacts) {
		this.contacts = contacts;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getId()
	 */
	@Override
	public long getId() {
		return id;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getCountry()
	 */
	@Override
	public String getCountry() {
		return country;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setCountry(java.lang.String)
	 */
	@Override
	public void setCountry(String country) {
		this.country = country;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getLatitude()
	 */
	@Override
	public String getLatitude() {
		return latitude;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setLatitude(java.lang.String)
	 */
	@Override
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getLongitude()
	 */
	@Override
	public String getLongitude() {
		return longitude;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setLongitude(java.lang.String)
	 */
	@Override
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getAdminUsername()
	 */
	@Override
	public String getAdminUsername() {
		return adminUsername;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setAdminUsername(java.lang.String)
	 */
	@Override
	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getRegionStatus()
	 */
	@Override
	@XmlTransient
	public IRegionStatus getRegionStatus() {
		return regionStatus;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setRegionStatus(org.fiteagle.api.IRegionStatus)
	 */
	@Override
	public void setRegionStatus(IRegionStatus registrationStatus) {
		
		this.regionStatus = registrationStatus;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#getNodeType()
	 */
	@Override
	public String getNodeType() {
		return nodeType;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setNodeType(java.lang.String)
	 */
	@Override
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#setId(long)
	 */
	@Override
	public void setId(long id) {
		this.id = id;
		if(null!= regionStatus){
			regionStatus.setRegion(id);
		}
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#addContact(org.fiteagle.xifi.api.model.ContactInformation)
	 */
	@Override
	public void addContact(IContactInformation contactInfo) {
		if(this.contacts == null)
			contacts = new ArrayList<>();
			
		contacts.add(contactInfo);
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#addLinksWithId(java.lang.String)
	 */
	@Override
	public void addLinksWithId(String uriInfo) {
		this.addLink("self",  new LinkInfo(trimURI(uriInfo)+ "/" + this.getId()));
		this.addLink("status" , new LinkInfo(trimURI(uriInfo)+ "/" + this.getId()+"/status/"));
		if(this.getContacts() != null && this.getContacts().size() > 0){
			for(IContactInformation contactInformation : this.getContacts()){
				this.addLink("contacts",  new LinkInfo(trimURI(uriInfo)+ "/" + this.getId()+"/contacts/" + contactInformation.getId()));
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegion#addLinksWithoutId(java.lang.String)
	 */
	@Override
	public void addLinksWithoutId(String uriInfo) {
		this.addLink("self",  new LinkInfo(trimURI(uriInfo)));
		this.addLink("status" , new LinkInfo(trimURI(uriInfo)+"/status/"));
		if(this.getContacts() != null && this.getContacts().size() > 0){
			for(IContactInformation contactInformation : this.getContacts()){
				this.addLink("contacts",  new LinkInfo(trimURI(uriInfo) + "/contacts/" + contactInformation.getId()));
			}
		}
		
	}
	
	

}
