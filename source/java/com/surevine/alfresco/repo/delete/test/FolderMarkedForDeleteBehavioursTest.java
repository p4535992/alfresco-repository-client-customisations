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
package com.surevine.alfresco.repo.delete.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.surevine.alfresco.repo.delete.FolderMarkedForDeleteBehaviours;
import com.surevine.alfresco.repo.delete.ManagedDeletionService;

public class FolderMarkedForDeleteBehavioursTest {

	@Mock
	NodeService nodeService;

	@Mock
	PolicyComponent policyComponent;

	@Mock
	ManagedDeletionService managedDeletionService;

	/**
	 * Class under test
	 */
	FolderMarkedForDeleteBehaviours behaviour;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		behaviour = new FolderMarkedForDeleteBehaviours();
		behaviour.setPolicyComponent(policyComponent);
		behaviour.setManagedDeletionService(managedDeletionService);
	}

	@Test
	public void testOnDeleteChildAssociationExistingNode() {
		NodeRef parentNodeRef = new NodeRef("store:///1");
		NodeRef childNodeRef = new NodeRef("store:///2");

		ChildAssociationRef childAssociationRef = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, parentNodeRef, null, childNodeRef);
		
		behaviour.onCreateChildAssociation(childAssociationRef, false);

		verify(managedDeletionService).folderContentsChanged(parentNodeRef);
		verify(managedDeletionService, times(0)).removeDeletionMark(any(NodeRef.class));
	}

	@Test
	public void testOnDeleteChildAssociationNewNode() {
		NodeRef parentNodeRef = new NodeRef("store:///1");
		NodeRef childNodeRef = new NodeRef("store:///2");

		ChildAssociationRef childAssociationRef = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, parentNodeRef, null, childNodeRef);

		behaviour.onCreateChildAssociation(childAssociationRef, true);

		verify(managedDeletionService).folderContentsChanged(parentNodeRef);
		verify(managedDeletionService, times(0)).removeDeletionMark(any(NodeRef.class));
	}

	@Ignore
	@Test
	public void testOnCreateChildAssociation() {
		NodeRef parentNodeRef = new NodeRef("store:///1");
		NodeRef childNodeRef = new NodeRef("store:///2");

		ChildAssociationRef childAssociationRef = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, parentNodeRef, null, childNodeRef);

		behaviour.onDeleteChildAssociation(childAssociationRef);

		verify(managedDeletionService).folderContentsChanged(parentNodeRef);
		verify(managedDeletionService, times(0)).removeDeletionMark(any(NodeRef.class));
	}

}
