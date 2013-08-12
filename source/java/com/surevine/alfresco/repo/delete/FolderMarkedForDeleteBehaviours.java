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
