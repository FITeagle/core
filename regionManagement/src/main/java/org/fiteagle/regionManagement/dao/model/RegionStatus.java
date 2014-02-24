package org.fiteagle.regionManagement.dao.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlType;

import org.fiteagle.api.ILinkInfo;
import org.fiteagle.api.IRegionStatus;

@XmlType
@Entity
public class RegionStatus extends LinkableEntity implements IRegionStatus{

	@Id
	long region;
	
	
	long timestamp;
	
	String status;

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#getTimestamp()
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#setTimestamp(long)
	 */
	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#getStatus()
	 */
	@Override
	public String getStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#setStatus(java.lang.String)
	 */
	@Override
	public void setStatus(String status) {
		this.status = status;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#getRegion()
	 */
	@Override
	public long getRegion() {
		return region;
	}
	


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#setRegion(long)
	 */
	@Override
	public void setRegion(long id) {
		this.region = id;
	}
	@Transient
	Map<String, List<ILinkInfo>> _links;
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#get_links()
	 */
	@Override
	public Map<String,List<ILinkInfo>> get_links() {
		return _links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#set_links(java.util.Map)
	 */
	@Override
	public void set_links(Map<String, List<ILinkInfo>> links) {
		this._links = links;
	}
	
	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#addLink(java.lang.String, org.fiteagle.xifi.api.model.LinkInfo)
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
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#equals(java.lang.Object)
	 */
	
	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if(this.getClass().equals(obj.getClass())){
			IRegionStatus toCompare = (IRegionStatus) obj;
			if(this.region == toCompare.getRegion() && this.getStatus().equals(toCompare.getStatus()) && this.getTimestamp() == toCompare.getTimestamp()){
				ret = true;
			}
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#addLinksWithId(java.lang.String)
	 */
	
	@Override
	public void addLinksWithId(String uriInfo) {
		
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.IRegionStatus#addLinksWithoutId(java.lang.String)
	 */
	
	@Override
	public void addLinksWithoutId(String uriInfo) {
		String trimmedUri = trimURI(uriInfo);
		this.addLink("self",  new LinkInfo(trimmedUri));
		this.addLink("parent", new LinkInfo(trimmedUri.subSequence(0, trimmedUri.lastIndexOf("/")).toString()));
		
	}
}
