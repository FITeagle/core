package org.fiteagle.regionManagement.dao.model;

import org.fiteagle.api.ILinkableEntity;


public abstract class LinkableEntity implements ILinkableEntity {


	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.ILinkableEntity#addLinksWithId(java.lang.String)
	 */
	@Override
	public abstract void addLinksWithId(String uriInfo);

	/* (non-Javadoc)
	 * @see org.fiteagle.xifi.api.model.ILinkableEntity#addLinksWithoutId(java.lang.String)
	 */
	@Override
	public abstract void addLinksWithoutId(String uriInfo);

	protected String trimURI(String uri) {

		if (uri.lastIndexOf("/") == uri.length() - 1) {
			uri = uri.subSequence(0, uri.length() - 1).toString();
		}
		return uri;
	}
}
