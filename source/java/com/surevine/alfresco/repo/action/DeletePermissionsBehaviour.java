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
package com.surevine.alfresco.repo.action;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.ModelDAO;
import org.alfresco.repo.security.permissions.impl.PermissionsDaoComponent;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;

public class DeletePermissionsBehaviour implements NodeServicePolicies.OnCreateNodePolicy {

	private final Logger LOGGER = Logger.getLogger(DeletePermissionsBehaviour.class);

	private NodeService _nodeService; 
	public void setNodeService(NodeService ns) {
		_nodeService=ns;
	}
	
	private PermissionService _permissionService;
	public void setPermissionService(PermissionService ps) {
		_permissionService=ps;
	}
	
	private PolicyComponent _policyComponent;
	public void setPolicyComponent(PolicyComponent pc) {
		_policyComponent=pc;
	}
    
	private ModelDAO _modelDAO;
	public void setModelDAO(ModelDAO md) {
		_modelDAO=md;
	}
	
	private PermissionsDaoComponent _permissionsDaoComponent;
	public void setPermissionsDaoComponent(PermissionsDaoComponent pdc) {
		_permissionsDaoComponent=pdc;
	}
	
	private TenantService _tenantService;
	public void setTenantService(TenantService ts) {
		_tenantService=ts;
	}
	
	private TransactionService _transactionService;
	public void setTransactionService(TransactionService ts) {
		_transactionService=ts;
	}
	
	private Behaviour onCreateNode;
	
	public void init() {
		LOGGER.info("Initialising");
		this.onCreateNode = new JavaBehaviour(this,
				"onCreateNode",
				NotificationFrequency.TRANSACTION_COMMIT);
		
		this._policyComponent.bindClassBehaviour(
				QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
				ContentModel.TYPE_CONTENT,
				this.onCreateNode);
		
		this._policyComponent.bindClassBehaviour(
				QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
				ContentModel.TYPE_FOLDER,
				this.onCreateNode);

	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childRef) {
		NodeRef nodeRef = childRef.getChildRef();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Running behaviour on parent: "+childRef.getParentRef()+" for child "+nodeRef);
		}
		setPermissions(nodeRef);
		LOGGER.debug("Permissions Set");
	}
	
	protected boolean isInDocumentLibrary(NodeRef nodeRef) {
		
		Path path = _nodeService.getPath(nodeRef);
		if (path==null) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Path for "+nodeRef+" was null");
			}
			return false;
		}
		String displayPath = _nodeService.getPath(nodeRef).toDisplayPath(_nodeService, _permissionService);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Display path of "+nodeRef+" is "+displayPath);
		}
		if (displayPath==null) {
			return false;
		}
		return (displayPath.indexOf("/documentLibrary")!=-1);
	}
	
	protected void setPermissions(NodeRef nodeRef) {
		try {
			if (nodeRef != null && _nodeService.exists(nodeRef) && isInDocumentLibrary(nodeRef)) {	
				final boolean setAllow = _nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER);
				
				
				setPermission(nodeRef, "GROUP_EVERYONE", _modelDAO.getPermissionReference(null, PermissionService.DELETE), setAllow);
			}
		} catch (Exception e) { //If an exception occurs here then still allow the creation of the node and log a warning
			LOGGER.error(e.getMessage(), e);
		}
	}

    private void setPermission(final NodeRef nodeRef, final String authority, final PermissionReference perm, final boolean allow)  {
    	
    	final RetryingTransactionHelper transactionHelper = _transactionService.getRetryingTransactionHelper();
    	transactionHelper.setMaxRetries(3);
    	transactionHelper.setMaxRetryWaitMs(3000);
    	
    	transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Boolean>(){
			public Boolean execute() throws Throwable {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Setting delete permission to %b for %s.", allow, nodeRef));
				}
				_permissionsDaoComponent.setPermission(_tenantService.getName(nodeRef), authority, perm, allow);
				return Boolean.TRUE;
			}
		},false,true);
    }
}
