package org.fiteagle.regionManagement.dao.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.fiteagle.api.IContactInformation;
import org.fiteagle.api.ILinkInfo;
import org.fiteagle.api.IRegion;

@XmlType
@Entity
public class ContactInformation extends LinkableEntity implements IContactInformation{

	@Id
	@Column(name="ID")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	long id;	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getId()
	 */
	@Override
	public long getId() {
		return id;
	}


	String name;
	String country;
	String fax;
	String phone;
	String email;
	String type;
	@ManyToOne(targetEntity=Region.class)
	@XmlTransient
	IRegion region;
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getType()
	 */
	@Override
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setType(java.lang.String)
	 */
	@Override
	public void setType(String type) {
		this.type = type;
	}

	String address;

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getCountry()
	 */
	@Override
	public String getCountry() {
		return country;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setCountry(java.lang.String)
	 */
	@Override
	public void setCountry(String country) {
		this.country = country;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getFax()
	 */
	@Override
	public String getFax() {
		return fax;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setFax(java.lang.String)
	 */
	@Override
	public void setFax(String fax) {
		this.fax = fax;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getPhone()
	 */
	@Override
	public String getPhone() {
		return phone;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setPhone(java.lang.String)
	 */
	@Override
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getEmail()
	 */
	@Override
	public String getEmail() {
		return email;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setEmail(java.lang.String)
	 */
	@Override
	public void setEmail(String email) {
		this.email = email;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#getAddress()
	 */
	@Override
	public String getAddress() {
		return address;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#setAddress(java.lang.String)
	 */
	@Override
	public void setAddress(String address) {
		this.address = address;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean ret =false;
		if(obj.getClass().equals(this.getClass())){
			IContactInformation toCompare  = (IContactInformation) obj;
			if(this.getAddress().equals(toCompare.getAddress())&&
			   this.getCountry().equals(toCompare.getCountry())&&
			   this.getEmail().equals(toCompare.getEmail())&&
			   this.getFax().equals(toCompare.getFax())&&
			   this.getName().equals(toCompare.getName())&&
			   this.getPhone().equals(toCompare.getPhone())&&
			   this.getType().equals(toCompare.getType())){
				ret = true;
			}
		}
		return ret;
	}
	

	@Transient
	Map<String, List<ILinkInfo>> _links;
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#get_links()
	 */
	@Override
	public Map<String,List<ILinkInfo>> get_links() {
		return _links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#set_links(java.util.Map)
	 */
	@Override
	public void set_links(Map<String, List<ILinkInfo>> links) {
		this._links = links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#addLink(java.lang.String, org.fiteagle.xifi.api.model.LinkInfo)
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

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#addLinksWithId(java.lang.String)
	 */
	@Override
	public void addLinksWithId(String uriInfo) {
		this.addLink("self",  new LinkInfo(trimURI(uriInfo)+ "/" + this.getId()));
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IContactInformation#addLinksWithoutId(java.lang.String)
	 */
	@Override
	public void addLinksWithoutId(String uriInfo) {
	
		
	}
	
}
