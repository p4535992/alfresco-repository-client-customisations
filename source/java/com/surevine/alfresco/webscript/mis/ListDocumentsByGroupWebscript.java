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
package com.surevine.alfresco.webscript.mis;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.surevine.esl.EnhancedSecurityLabel;

public class ListDocumentsByGroupWebscript extends AbstractWebScript {
	
	private static final Logger LOG = Logger.getLogger(ListDocumentsByGroupWebscript.class);
	
	private static final List<QName> DOCUMENT_TYPES = Arrays.asList(new QName[] {
		ContentModel.TYPE_CONTENT, ForumModel.TYPE_POST });
	
	private static final List<QName> CONTAINER_TYPES = Arrays.asList(new QName[] {
		ContentModel.TYPE_FOLDER, ForumModel.TYPE_TOPIC });
	
	private static final Set<QName> CHILD_TYPES = new HashSet<QName>();
	
	static {
		CHILD_TYPES.addAll(DOCUMENT_TYPES);
		CHILD_TYPES.addAll(CONTAINER_TYPES);
	}

	private NodeService _nodeService; 
	
	private SiteService _siteService; 
	
	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}
	
	public void setSiteService(final SiteService siteService) {
		_siteService = siteService;
	}
	
	@Override
	public void execute(final WebScriptRequest request, final WebScriptResponse response) throws IOException {
		LOG.info(String.format("Starting %s", ListDocumentsByGroupWebscript.class.getName()));
		
		final long start = System.currentTimeMillis();
		
		final String groupType = request.getParameter("type").toLowerCase();
		final String groupName = request.getParameter("name").toUpperCase();
		
		final StringBuilder html = new StringBuilder(500000);
		
		try {
			final List<SiteInfo> sites = _siteService.listSites(AuthenticationUtil.getRunAsUser());
			
			for (final SiteInfo site : sites) {
				if (!site.getShortName().endsWith("deletedItems") && !site.getShortName().equals("rm")) {
					final List<NodeRef> results = recurseChildNodes(groupType, groupName,
							_nodeService.getChildAssocs(site.getNodeRef(), CHILD_TYPES));

					for (final NodeRef result : results) {
						html.append(result);
						html.append("|");
						html.append(_nodeService.getProperty(result, ContentModel.PROP_NAME));
						html.append("<br/>");
					}
				}
			}
			
			writeHTML(response, 200,
					String.format("<html><head><title>Alfresco</title></head><body>%s</body></html>", html.toString()));
			
			LOG.info(String.format("Completed %s successfully in %dms.",
					ListDocumentsByGroupWebscript.class.getName(), (System.currentTimeMillis() - start)));
		} catch (final Exception e) {
			writeHTML(response, 500,
					"<html><head><title>Alfresco</title></head><body>Internal Server Error.</body></html>");
		}
	}
	
	private List<NodeRef> recurseChildNodes(final String groupType,
			final String groupName, final List<ChildAssociationRef> children) {
		final List<NodeRef> results = new ArrayList<NodeRef>(1000);
		
		for (final ChildAssociationRef child : children) {
			final QName childType = _nodeService.getType(child.getChildRef());
			
			if (DOCUMENT_TYPES.contains(childType)) {
				if (matchesGroup(groupType, groupName, child)) {
					results.add(child.getChildRef());
				}
			} else if (CONTAINER_TYPES.contains(childType)) {
				final List<ChildAssociationRef> childNodes = _nodeService.getChildAssocs(child.getChildRef(), CHILD_TYPES);
				if (childNodes.size() > 0) {
					results.addAll(recurseChildNodes(groupType, groupName, childNodes));
				}
			}
		}
		
		return results;
	}
	
	private boolean matchesGroup(final String groupType, final String groupName, final ChildAssociationRef child) {
		final Map<QName, Serializable> properties = _nodeService.getProperties(child.getChildRef());
		
		if (groupType.equals("organisation")) {
			return matchesByQName(properties, EnhancedSecurityLabel.ORGANISATIONS, groupName);
		} else if (groupType.equals("closed")) {
			return matchesByQName(properties, EnhancedSecurityLabel.CLOSED_GROUPS, groupName);
		}
		
		return false;
	}
	
	private boolean matchesByQName(final Map<QName, Serializable> properties,
			final QName type, final String groupName) {
		@SuppressWarnings("unchecked")
		final List<String> groups = (List<String>) properties.get(type);
		
		return groups != null && groups.contains(groupName);
	}
	
	private void writeHTML(final WebScriptResponse response, final int status,
			final String content) throws IOException {
		response.setStatus(status);
		response.setContentType("text/html");
		response.getWriter().write(content);
		response.getWriter().flush();
	}
}
