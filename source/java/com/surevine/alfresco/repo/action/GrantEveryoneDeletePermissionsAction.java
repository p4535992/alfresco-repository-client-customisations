package com.surevine.alfresco.repo.action;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Really simple action designed to work around the fact that ALF-8194 effectively prevents the creation of new permission groups
 * by side-stepping the "proper" way of doing things and granting delete permission explicitly
 * @author simonw
 */
public class GrantEveryoneDeletePermissionsAction extends ActionExecuterAbstractBase 
{

	private static final Log LOGGER = LogFactory.getLog(GrantEveryoneDeletePermissionsAction.class);
	
	private PermissionService _permissionService;
	
	private NodeService _nodeService;
	
	private TransactionService _transactionService;
	
	private static volatile Integer _counter=0;
	
	public void setPermissionService(PermissionService permissionService) {
		_permissionService = permissionService;
	}
	
	public void setNodeService(NodeService nodeService) {
		_nodeService = nodeService;
	}
	
	public void setTransactionService(TransactionService transactionService) {
		_transactionService = transactionService;
	}
	
	@Override
	protected synchronized void executeImpl(final Action action, final NodeRef nodeRef) {
		try {
			if (nodeRef != null && _nodeService.exists(nodeRef)) {	
				final boolean setAllow = _nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER);
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Setting delete permission to %b for %s.", setAllow, nodeRef));
				}
				synchronized (_counter) {
					_counter++;
				}
				_transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Boolean>(){
					public Boolean execute() throws Throwable {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(_counter+" threads in progress");
						}
						try {
							_permissionService.setPermission(nodeRef, "GROUP_EVERYONE", PermissionService.DELETE, setAllow);
							LOGGER.debug("Permissions Set");
						}
						catch (Throwable t) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("Throwable "+t.getClass()+" caught in retrying transaction");
							}
							throw t;
						}
						finally {
							_counter--;
						}
						
						return Boolean.TRUE;
					}
				}, false, true);
			}
		} catch (Exception e) { //If an exception occurs here then still allow the creation of the node and log a warning
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) { //Intentionally left blank
	}
}