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
package com.surevine.alfresco.repo.action;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action to fix folders with explicit delete permissions by clearing the delete permission from the folder and
 * @author simonw
 *
 */
public class FixFolderPermissionsAction extends ActionExecuterAbstractBase {
	
	/**
	 * The id of the spring bean this class is instantiated under, which according to our standards will be the fqName of this class.  This is awkward, but
	 * seems to be reqd (certainly, it's how the core Alfresco code works) in order to wrap this action executor into an Action via ActionService, which we 
	 * need to do to execute it in java using ActionService
	 */
	public static final String NAME = "com.surevine.alfresco.repo.action.FixFolderPermissionsAction";

	private static final Log LOGGER = LogFactory.getLog(FixFolderPermissionsAction.class);

	private PermissionService _permissionService;

	private NodeService _nodeService;

	public void setPermissionService(PermissionService permissionService) {
		_permissionService = permissionService;
	}

	public void setNodeService(NodeService nodeService) {
		_nodeService = nodeService;
	}

	@Override
	protected synchronized void executeImpl(final Action action, final NodeRef nodeRef) {

		if (nodeRef != null && _nodeService.exists(nodeRef)) { //Only operate on nodes that exist
			final boolean isFolder = _nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Is %s a folder?  %b", nodeRef, isFolder));
			}
			if (isFolder) { //Only operate on folders
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(String.format("Clearing delete permissions of %s", nodeRef));
				}
				_permissionService.setPermission(nodeRef, "GROUP_EVERYONE", PermissionService.DELETE, false); //Remove the delete permission
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(String.format("Cleared delete permissions of %s", nodeRef));
				}
			}
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) { // Intentionally
																				// left
																				// blank
	}
}