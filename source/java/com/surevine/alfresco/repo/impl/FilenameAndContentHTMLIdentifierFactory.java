/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *   
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
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
