package org.fiteagle.regionManagement.dao.model;

import org.fiteagle.api.ILinkInfo;

public class LinkInfo implements ILinkInfo {

	
	private String href;

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.ILinkInfo#getHref()
	 */
	@Override
	public String getHref() {
		return href;
	}

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.ILinkInfo#setHref(java.lang.String)
	 */
	@Override
	public void setHref(String href) {
		this.href = href;
	}

	public LinkInfo( String href) {
		this.href = href;
	}


}
