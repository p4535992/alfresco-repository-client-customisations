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
package com.surevine.alfresco.webscript.cluster;

import java.io.IOException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class HealtCheckWebscript extends AbstractWebScript {
	
	private static final StoreRef STORE_REF = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	private SearchService _searchService;
	private NodeService _nodeService;
	
	public void setSearchService(SearchService searchService)
	{
		_searchService=searchService;
	}
	
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	
	@Override
	public void execute(WebScriptRequest request, WebScriptResponse response) throws IOException 
	{
		try 
		{
			AuthenticationUtil.runAs(new HealthCheckImpl(), AuthenticationUtil.getAdminUserName());
			response.setStatus(200);
			response.setContentType("text/html");
			response.getWriter().write("<html><head><title>Alfresco</title></head><body>OK</body></html>");
			response.getWriter().flush();
		}
		catch (Exception e)
		{
			response.setStatus(500);
			response.setContentType("text/html");
			response.getWriter().write("<html><head><title>Alfresco</title></head><body>FAIL</body></html>");
			response.getWriter().flush();
		}
	}
			

	private class HealthCheckImpl implements RunAsWork<Boolean>
	{
		@Override
		public Boolean doWork() throws Exception {
			ResultSet rs = null;
			try {
				rs = _searchService.query(STORE_REF, SearchService.LANGUAGE_LUCENE, "+PATH:\"/{http://www.alfresco.org/model/application/1.0}company_home/\"");
				NodeRef nr = rs.getRow(0).getNodeRef();
				if ("Company Home".equalsIgnoreCase((String) _nodeService.getProperty(nr, ContentModel.PROP_NAME)))
				{
					throw new RuntimeException("Expected [Company Home] not found");
				}
				return Boolean.TRUE;
			}
			finally {
				if (rs!=null) {
					rs.close();
				}
			}
		}
	}

}
