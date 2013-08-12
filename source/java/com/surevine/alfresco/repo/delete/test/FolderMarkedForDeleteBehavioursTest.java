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
