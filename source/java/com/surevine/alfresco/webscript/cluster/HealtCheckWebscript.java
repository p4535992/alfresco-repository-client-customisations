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