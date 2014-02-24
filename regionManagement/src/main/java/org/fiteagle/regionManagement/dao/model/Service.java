package org.fiteagle.regionManagement.dao.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

import org.fiteagle.api.ILinkInfo;
import org.fiteagle.api.IService;


@XmlRootElement
@Entity
@NamedQueries({
	@NamedQuery(name="Service.findAll", query="SELECT s FROM Service s"),
	@NamedQuery(name="Service.findByType", query="SELECT s FROM Service s WHERE s.type = :type")
})
public class Service extends LinkableEntity implements IService{

	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	long id;
	
	String type;

	@Transient
	Map<String,ILinkInfo> _links;

	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#get_links()
	 */
	@Override
	public Map<String, ILinkInfo> get_links() {
		return _links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#set_links(java.util.Map)
	 */
	@Override
	public void set_links(Map<String, ILinkInfo> links) {
		this._links = links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#addLink(java.lang.String, org.fiteagle.xifi.api.model.LinkInfo)
	 */
	@Override
	public void addLink(String name,ILinkInfo l){
		if(this._links == null)
			_links = new HashMap<>();
			
		_links.put(name,l);
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#getType()
	 */
	@Override
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#setType(java.lang.String)
	 */
	@Override
	public void setType(String type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#getId()
	 */
	@Override
	public long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#setId(long)
	 */
	@Override
	public void setId(long serviceId) {
		this.id = serviceId;
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#addLinksWithId(java.lang.String)
	 */
	@Override
	public void addLinksWithId(String uriInfo) {
		this.addLink("self",  new LinkInfo(trimURI(uriInfo)+ "/" + this.getId()));
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IService#addLinksWithoutId(java.lang.String)
	 */
	@Override
	public void addLinksWithoutId(String uriInfo) {
		// TODO Auto-generated method stub
		
	}
	
	
}
