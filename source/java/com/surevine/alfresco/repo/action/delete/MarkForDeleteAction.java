package com.surevine.alfresco.repo.action.delete;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.repo.delete.ManagedDeletionService;

/**
 * Action executor to mark items for deletion, which delegates to ManagedDeletionService
 * @author simonw
 *
 */
public class MarkForDeleteAction extends ActionExecuterAbstractBase {

	
	private static final Log _logger = LogFactory.getLog(MarkForDeleteAction.class);
	
	private ManagedDeletionService _mdService;
	
	public void setManagedDeletionService(ManagedDeletionService mdService)
	{
		_mdService=mdService;
	}
	
	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Marking "+nodeRef+" for deletion from action: "+action.getTitle());
		}
		_mdService.markForDelete(nodeRef);
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters available
	}

}
