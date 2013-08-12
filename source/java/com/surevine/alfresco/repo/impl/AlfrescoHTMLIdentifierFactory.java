package com.surevine.alfresco.repo.impl;

import org.alfresco.service.cmr.repository.ContentService;

import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.HTMLIdentifierFactory;
import org.alfresco.service.cmr.repository.ContentService;

public class AlfrescoHTMLIdentifierFactory implements HTMLIdentifierFactory {

	//Injected contentService used to get ContentAccessor, to get mimetype
	private ContentService _contentService;
	
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	public HTMLIdentifier getHTMLIdentifier() {
		return new AlfrescoHTMLIdentifier(_contentService);
	}

}
