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
package com.surevine.alfresco.repo.delete;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * Encapsulates some policy around the {@link ManagedDeletionModel#ASPECT_FOLDER_MARKED_FOR_DELETION} aspect.
 * It defines the following behaviours:
 * <ul><li>Removes the folder deletion mark if content is added or removed from the folder
 * </ul>
 */
public class FolderMarkedForDeleteBehaviours implements
		NodeServicePolicies.OnCreateChildAssociationPolicy,
		NodeServicePolicies.OnDeleteChildAssociationPolicy {
	private final Logger _logger = Logger.getLogger(FolderMarkedForDeleteBehaviours.class);
	
	// Dependencies
	private PolicyComponent _policyComponent;

	public void setPolicyComponent(final PolicyComponent policyComponent) {
		_policyComponent = policyComponent;
	}

	private ManagedDeletionService _managedDeletionService;

	public void setManagedDeletionService(
			final ManagedDeletionService managedDeletionService) {
		_managedDeletionService = managedDeletionService;
	}

	// Behaviours
	private Behaviour onDeleteChildAssociation;
	private Behaviour onCreateChildAssociation;

	public void init() {
		_logger.info("Initialising 'Remove Folder When Empty' behaviours");
		this.onCreateChildAssociation = new JavaBehaviour(this,
				"onCreateChildAssociation",
				NotificationFrequency.TRANSACTION_COMMIT);

		this.onDeleteChildAssociation = new JavaBehaviour(this,
				"onDeleteChildAssociation",
				NotificationFrequency.TRANSACTION_COMMIT);

		this._policyComponent.bindAssociationBehaviour(
				QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateChildAssociation"),
				ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION,
				this.onCreateChildAssociation);

		this._policyComponent.bindAssociationBehaviour(
				QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteChildAssociation"),
				ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION,
				this.onDeleteChildAssociation);
	}

	@Override
	public void onDeleteChildAssociation(ChildAssociationRef childAssocRef) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("Child Association Deleted - removing the deletion mark");
		}
		
		_managedDeletionService.folderContentsChanged(childAssocRef.getParentRef());
	}

	@Override
	public void onCreateChildAssociation(ChildAssociationRef childAssocRef,
			boolean isNewNode) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("Child Association Created - removing the deletion mark");
		}

		_managedDeletionService.folderContentsChanged(childAssocRef.getParentRef());
	}

}
