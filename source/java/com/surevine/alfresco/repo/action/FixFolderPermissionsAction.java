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
				_permissionService.clearPermission(nodeRef, PermissionService.DELETE); //Remove the delete permission
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