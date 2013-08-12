package com.surevine.alfresco.repo.impl;

import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;

import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.HTMLIdentifierFactory;

public class FilenameAndContentHTMLIdentifierFactory implements
		HTMLIdentifierFactory {

	private String[] _htmlExtensions;
	public void setHTMLExtensions(String[] htmlExtensions)
	{
		_htmlExtensions=FilenameAndContentHTMLIdentifier.StringArrayToLowerCase(htmlExtensions);
	}
	
	private String[] _htmlContent;
	public void setHTMLContent(String[] htmlContent)
	{
		_htmlContent=FilenameAndContentHTMLIdentifier.StringArrayToLowerCase(htmlContent);
	}
	
	private int _maxCharsToSearch;
	public void setMaxCharsToSearch(int maxCharsToSearch)
	{
		_maxCharsToSearch=maxCharsToSearch;
	}
	
	private NodeService _nodeService;
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	private ContentService _contentService;
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	public HTMLIdentifier getHTMLIdentifier() {
		return new FilenameAndContentHTMLIdentifier(_contentService, _nodeService, _htmlExtensions, _htmlContent, _maxCharsToSearch);
	}

}
