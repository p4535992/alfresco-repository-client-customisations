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
package com.surevine.alfresco.repo.action.delete;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OnDeleteCleanVersionHistoryBehaviour implements NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.BeforeDeleteNodePolicy  {

	private static final Log _logger = LogFactory.getLog(OnDeleteCleanVersionHistoryBehaviour.class);

	private PolicyComponent policyComponent;
	private Behaviour beforeDeleteNode;
	private Behaviour onDeleteNode;
	private NodeService nodeService;
	private VersionService versionService;
	private NodeArchiveService archiveService;
	
	public void setNodeService(NodeService ns) {
		nodeService=ns;
	}
	
	public void setVersionService(VersionService vs) {
		versionService=vs;
	}
	
	public void setPolicyComponent(PolicyComponent pc) {
		policyComponent=pc;
	}
	
	public void setArchiveService(NodeArchiveService nas) {
		archiveService=nas;
	}

	public void init() {

		this.beforeDeleteNode = new JavaBehaviour(this,"beforeDeleteNode",NotificationFrequency.EVERY_EVENT);
		this.onDeleteNode = new JavaBehaviour(this,"onDeleteNode",NotificationFrequency.EVERY_EVENT);
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI,"onDeleteNode"), ContentModel.TYPE_CONTENT, this.onDeleteNode);
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI,"beforeDeleteNode"), ContentModel.TYPE_CONTENT, this.beforeDeleteNode);
	}

	@Override
	public void onDeleteNode(ChildAssociationRef childAssocRef, boolean archived) {
		if (archived) {
			NodeRef nodeRef=childAssocRef.getChildRef();
			NodeRef archivedNodeRef = archiveService.getArchivedNode(nodeRef);
			if (nodeRef.getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)) {
				try 
				{
					if (_logger.isDebugEnabled()) {
						_logger.debug("Purging "+archivedNodeRef);
					}
					archiveService.purgeArchivedNode(archiveService.getArchivedNode(nodeRef));
					if (_logger.isDebugEnabled()) {
						_logger.debug("Succesfully performed purge on "+nodeRef);
					}
				}
				catch(Exception e) 
				{
					_logger.warn("Could not purge "+nodeRef+".  Unless the node is purged manually, the contents of the node will not be purged from the filesystem", e);
				}
			}
		}
	}

	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		try {
			if (_logger.isDebugEnabled()) {
				_logger.debug("Attempting to remove version history from " + nodeRef);
			}			
			
			// Remove the version history
			if(nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
				if (_logger.isTraceEnabled()) {
					_logger.trace("Removing version history from node "+nodeRef);
				}
				versionService.deleteVersionHistory(nodeRef);
				if (_logger.isTraceEnabled()) {
					_logger.trace("Succesfully removed version history from "+nodeRef);
				}
			}
		}
		catch (Exception e) {
			_logger.warn("Could not remove the version history of "+nodeRef+".  Unless the node is purged manually, the contents of the node will not be purged from the filesystem", e);
		}
	}
}
	
