package com.surevine.alfresco.repo.impl;

import org.alfresco.service.cmr.repository.NodeRef;
import com.surevine.alfresco.repo.HTMLIdentifier;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of HTMLIdentifier that trusts the mime type recorded by Alfresco to identify
 * whether or not a content item is HTML
 * @author SimonW
 */
public class AlfrescoHTMLIdentifier implements HTMLIdentifier {
	
	private static final QName CONTENT_QNAME = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
	private static final Log LOGGER = LogFactory.getLog(AlfrescoHTMLIdentifier.class);

	//Injected contentService used to get ContentAccessor, to get mimetype
	private ContentService _contentService;
	private NodeRef _target=null;
	
	
	public void setTargetNodeRef(NodeRef target){
		_target=target;
	}
	
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	public AlfrescoHTMLIdentifier()
	{
		
	}
	
	public AlfrescoHTMLIdentifier(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	public ContentService getContentService()
	{
		return _contentService;
	}
	
	public NodeRef getTargetNodeRef() 
	{
		return _target;
	}

	/**
	 * If the mimetype returned by the content service contains the string "htm" (case-insensitive),
	 * then it's an HTML file, otherwise it isn't
	 */
	public boolean isTargetHTML() throws IllegalStateException 
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Using 1.3.0 HTML Identifier(AlfrescoHTMLIdentifier)");
		}
		
		if (_target==null)
		{
			throw new IllegalStateException("isTargetHTML called but target not set");
		}
		
		ContentAccessor ca = _contentService.getReader(_target, CONTENT_QNAME);
		String mimeString = ca.getMimetype();
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("mimeType of "+_target+" is "+mimeString);
		}
		
		if (mimeString!=null && mimeString.toLowerCase().contains("htm"))
		{
			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug(_target+" is HTML");
			}
			return true;
		}
		
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(_target+" is not HTML");
		}
		return false;
	}
}
