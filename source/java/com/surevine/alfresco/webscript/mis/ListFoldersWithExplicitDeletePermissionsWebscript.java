/*
  Copyright (C) 2008-2014 Surevine Limited.
    
  Although intended for deployment and use alongside Alfresco this module should
  be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
  http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.surevine.alfresco.webscript.mis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import com.surevine.alfresco.repo.action.FixFolderPermissionsAction;

public class ListFoldersWithExplicitDeletePermissionsWebscript  extends AbstractWebScript {
	
	private static final Logger LOG = Logger.getLogger(ListFoldersWithExplicitDeletePermissionsWebscript.class);

	private NodeService _nodeService; 
	private SiteService _siteService; 
	private ActionService _actionService;
	private PermissionService _permissionService;

	
	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}
	
	public void setSiteService(final SiteService siteService) {
		_siteService = siteService;
	}
	
	public void setActionService(final ActionService actionService) {
		_actionService=actionService;
	}
	
	public void setPermissionService(final PermissionService permissionService) {
		_permissionService=permissionService;
	}
	
	@Override
	public void execute(final WebScriptRequest request, final WebScriptResponse response) throws IOException {
		if (LOG.isInfoEnabled()) {
			LOG.info(String.format("Starting %s", ListFoldersWithExplicitDeletePermissionsWebscript.class.getName()));
		}
		
		boolean operate=false;
		final String siteName = request.getParameter("site").toLowerCase(); //Get name of site from params
		if (request.getParameter("changePermissions")!=null && Boolean.valueOf(request.getParameter("changePermissions"))) {
			operate=true;
		}
		final StringBuilder html = new StringBuilder(500000);//Git buffer for results
		
		try {
			final SiteInfo site = _siteService.getSite(siteName);
			if (site==null) {
				LOG.error(String.format("Could not find a site with the specified name %s", siteName));
				throw new RuntimeException(String.format("Could not find a site with the specified name %s", siteName));
			}
			
			
			final List<NodeRef> results = recurseChildFolders(_nodeService.getChildAssocs(site.getNodeRef()), operate);

			for (final NodeRef result : results) {
				html.append(result);
				html.append("|");
				html.append(_nodeService.getProperty(result, ContentModel.PROP_NAME));
				html.append("<br/>");
			}
			
			
			writeHTML(response, 200,
					String.format("<html><head><title>Alfresco</title></head><body>%s</body></html>", html.toString()));
			
			LOG.info(String.format("Completed %s successfully",	ListFoldersWithExplicitDeletePermissionsWebscript.class.getName()));
			
		} catch (final Exception e) {
			writeHTML(response, 500,
					"<html><head><title>Alfresco</title></head><body>Internal Server Error.</body></html>");
			LOG.error("Exception caught attempting to enumerate folders with incorrect permissions", e);
		}
	}
	
	private List<NodeRef> recurseChildFolders(final List<ChildAssociationRef> children, boolean operate) {
		final List<NodeRef> results = new ArrayList<NodeRef>(1000);
		
		for (final ChildAssociationRef child : children) {
			final NodeRef childNode = child.getChildRef();
			final QName childType = _nodeService.getType(childNode);
			
			
			if (childType.equals(ContentModel.TYPE_FOLDER) && hasDeletePermission(childNode) ) {
				
				results.add(childNode);
				if (LOG.isInfoEnabled()) {
					LOG.info("Found a folder with explicit delete permissions: "+childNode);
				}
				if (operate) {
					operateOn(childNode);
				}
				final List<ChildAssociationRef> childNodes = _nodeService.getChildAssocs(childNode);
				if (childNodes.size() > 0) {
					results.addAll(recurseChildFolders(childNodes, operate));
				}
			}
		}
		return results;
	}
	
	protected boolean hasDeletePermission(NodeRef node) {
		final boolean trace = LOG.isTraceEnabled();
		if (trace) {
			LOG.trace("Does "+node+" have delete permissions?");
		}
		Iterator<AccessPermission> permissions = _permissionService.getPermissions(node).iterator();
		while (permissions.hasNext()) {
			AccessPermission permission = permissions.next();
			if (trace) {
				LOG.trace(String.format("NODE: %s | DIRECT: %s | PERM: %s | ACCESS: %s", node, permission.isSetDirectly(), permission.getPermission(), permission.getAccessStatus()));
			}
			if (permission.isSetDirectly() && permission.getPermission().equals(PermissionService.DELETE) && permission.getAccessStatus().equals(AccessStatus.ALLOWED)) {
				return true;
			}
		}
		return false;
	}
	
	protected void operateOn(NodeRef nodeRef) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Operating on %s", nodeRef));
		}
		Action action = _actionService.createAction(FixFolderPermissionsAction.NAME);
		try {
    		AuthenticationUtil.runAs( new ExecuteJobWork(action, nodeRef, _actionService), AuthenticationUtil.getSystemUserName());
    	}
    	catch (Throwable e)
    	{
    		if (e.getCause()!=null)
    		{
    			e=e.getCause();
    		}
    		LOG.warn("Could not fix permissions of "+nodeRef+" due to: "+e, e);
    	}
	}
	
	private void writeHTML(final WebScriptResponse response, final int status,
			final String content) throws IOException {
		response.setStatus(status);
		response.setContentType("text/html");
		response.getWriter().write(content);
		response.getWriter().flush();
	}
	
	private class ExecuteJobWork implements RunAsWork<Boolean>
	{
		private Action _action;
		private NodeRef _target;
		private ActionService _actionService;
		public ExecuteJobWork(Action action, NodeRef target, ActionService actionService)
		{
			_action=action;
			_target=target;
			_actionService=actionService;
		}
		public Boolean doWork() throws Exception {
    		_actionService.executeAction(_action, _target); //Delete the item - we log and ignore errors to prevent one corrupt item from stopping the entire run
    		return Boolean.TRUE;
		}
	}
}