package org.fiteagle.regionManagement.dao.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;

import org.fiteagle.api.IEndpoint;
import org.fiteagle.api.ILinkInfo;

@Entity
public class Endpoint implements IEndpoint {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	@XmlElement(name="interface")
	private String interfaceType;
	private long service_id;
	private String name;
	private String url;
	@Transient
	private Map<String,ILinkInfo> _links;	
	@XmlElement(name="region")
	private long regionId;
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getId()
	 */
	@Override
	public long getId() {
		return id;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setId(long)
	 */
	@Override
	public void setId(long id) {
		this.id = id;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getInterfaceType()
	 */
	@Override
	@XmlElement(name="interface")
	public String getInterfaceType() {
		return interfaceType;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setInterfaceType(java.lang.String)
	 */
	@Override
	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getService_id()
	 */
	@Override
	public long getService_id() {
		return service_id;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setService_id(long)
	 */
	@Override
	public void setService_id(long serviceId) {
		this.service_id = serviceId;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#get_links()
	 */
	@Override
	public Map<String, ILinkInfo> get_links() {
		return _links;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#set_links(java.util.Map)
	 */
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#addLink(java.lang.String, org.fiteagle.xifi.api.model.LinkInfo)
	 */
	
	private void addLink(String name,ILinkInfo l){
		if(this._links == null)
			_links =  new HashMap<String, ILinkInfo>();
			
		_links.put(name,l);
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getRegionId()
	 */
	@Override
	public long getRegionId() {
		return regionId;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setRegionId(long)
	 */
	@Override
	public void setRegionId(long regionId) {
		this.regionId = regionId;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#getUrl()
	 */
	@Override
	public String getUrl() {
		return url;
	}
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IEndpoint#setUrl(java.lang.String)
	 */
	@Override
	public void setUrl(String url) {
		this.url = url;
	}
	@Override
	public void addLink(String name, String string) {
		addLink(name, new LinkInfo(string));
		
	}
	
	
	
	
	
	
	
}
