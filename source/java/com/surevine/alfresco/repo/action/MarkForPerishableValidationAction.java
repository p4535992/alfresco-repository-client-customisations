package com.surevine.alfresco.repo.action;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * Action executor to mark items for perishable reason validation.
 * 
 * @author richardm
 */
public class MarkForPerishableValidationAction extends ActionExecuterAbstractBase {
	
	private static final Log _logger = LogFactory.getLog(MarkForPerishableValidationAction.class);
	
	private NodeService _nodeService;
	
	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}
	
	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {
		if (_logger.isDebugEnabled()) {
			_logger.debug("Marking "+nodeRef+" for perishable reason validation");
		}
		
		if(!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_VALIDATE_PERISHABLE_REASONS)) {
			_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_VALIDATE_PERISHABLE_REASONS, null);
		} else if (_logger.isDebugEnabled()) {
			_logger.debug("Folder " + nodeRef + " already has the perishableReasons aspect");
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters available
	}
}
