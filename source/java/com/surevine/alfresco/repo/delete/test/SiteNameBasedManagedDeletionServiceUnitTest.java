package com.surevine.alfresco.repo.delete.test;

import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.surevine.alfresco.model.ManagedDeletionModel;
import com.surevine.alfresco.repo.NodeFinder;
import com.surevine.alfresco.repo.NodeFinder.SearchCase;
import com.surevine.alfresco.repo.delete.SiteNameBasedManagedDeletionService;
import com.surevine.alfresco.repo.delete.SiteNameBasedManagedDeletionService.PathElement;

@Ignore
public class SiteNameBasedManagedDeletionServiceUnitTest {

	@Mock
	NodeService _nodeService;

	@Mock
	FileFolderService _fileFolderService;

	@Mock
	NodeFinder _nodeFinder;

	@Mock
	BehaviourFilter _behaviourFilter;

	@Mock
	LockService _lockService;
	
	public static Path BASE_TEST_PATH = new Path();
	static 
	{
		BASE_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		BASE_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		BASE_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble"));
		BASE_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
	}


	/**
	 * Class under test
	 */
	SiteNameBasedManagedDeletionService _mds;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		_mds = new SiteNameBasedManagedDeletionService();

		_mds.setFileFolderService(_fileFolderService);
		_mds.setNodeFinder(_nodeFinder);
		_mds.setNodeService(_nodeService);
		_mds.setPolicyFilter(_behaviourFilter);
		_mds.setLockService(_lockService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteDisablesFolderAspectBehavioursWhileDeleting() {
		// +++ Given
		// We have a file
		NodeRef file = new NodeRef("store:///2");
		when(_nodeService.exists(file)).thenReturn(true);
		
		Path filePath = new Path();
		filePath.append(BASE_TEST_PATH);
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}folder"));
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}file"));
		
		when(_nodeService.getPath(file)).thenReturn(filePath);
		
		// And this is done so the method doesn't fail
		when(_nodeFinder.getNodeRef(any(Path.class), any(SearchCase.class))).thenReturn(new NodeRef("store:///x"));

		// And the file is marked for delete
		when(
				_nodeService.hasAspect(file,
						ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
				.thenReturn(true);

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The FolderMarkedForDeletion aspect behaviours are disabled
		verify(_behaviourFilter).disableBehaviour(
				ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
		
		// Double check there wasn't an error (as it doesn't throw an exception)
		verify(_nodeService, times(0)).addAspect(eq(file), eq(ManagedDeletionModel.ASPECT_FAILED_TO_DELETE), any(Map.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteRemovesParentFolderIfMarkedForDeleteAndEmptyAfterDelete() {
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		when(_nodeService.exists(file)).thenReturn(true);
		ChildAssociationRef assoc = new ChildAssociationRef(
				ContentModel.ASSOC_CONTAINS, folder, null, file);
		when(_nodeService.getPrimaryParent(file)).thenReturn(assoc);
		
		Path filePath = new Path();
		filePath.append(BASE_TEST_PATH);
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}folder"));
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}file"));
		
		when(_nodeService.getPath(file)).thenReturn(filePath);
		
		// And this is done so the method doesn't fail
		when(_nodeFinder.getNodeRef(any(Path.class), any(SearchCase.class))).thenReturn(new NodeRef("store:///x"));

		// And the folder is marked for delete
		when(
				_nodeService.hasAspect(folder,
						ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION))
				.thenReturn(true);

		// And the file is marked for delete
		when(
				_nodeService.hasAspect(file,
						ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
				.thenReturn(true);

		// And the folder is empty after delete
		when(
				_nodeService.getChildAssocs(folder,
						ContentModel.ASSOC_CONTAINS,
						RegexQNamePattern.MATCH_ALL)).thenReturn(
				new ArrayList<ChildAssociationRef>());

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The folder is destroyed
		verify(_nodeService).deleteNode(folder);
		
		// Double check there wasn't an error (as it doesn't throw an exception)
		verify(_nodeService, times(0)).addAspect(eq(file), eq(ManagedDeletionModel.ASPECT_FAILED_TO_DELETE), any(Map.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteDoesntRemoveParentFolderIfMarkedForDeleteAndNotEmptyAfterDelete() {
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		when(_nodeService.exists(file)).thenReturn(true);
		ChildAssociationRef assoc = new ChildAssociationRef(
				ContentModel.ASSOC_CONTAINS, folder, null, file);
		when(_nodeService.getPrimaryParent(file)).thenReturn(assoc);
		
		Path filePath = new Path();
		filePath.append(BASE_TEST_PATH);
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}folder"));
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}file"));
		
		when(_nodeService.getPath(file)).thenReturn(filePath);
		
		// And this is done so the method doesn't fail
		when(_nodeFinder.getNodeRef(any(Path.class), any(SearchCase.class))).thenReturn(new NodeRef("store:///x"));

		// And the folder is marked for delete
		when(
				_nodeService.hasAspect(folder,
						ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION))
				.thenReturn(true);

		// And the file is marked for delete
		when(
				_nodeService.hasAspect(file,
						ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
				.thenReturn(true);

		// And the folder is not empty after delete
		ArrayList<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>();
		assocs.add(new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder,
				null, new NodeRef("store:///3")));
		when(
				_nodeService.getChildAssocs(folder,
						ContentModel.ASSOC_CONTAINS,
						RegexQNamePattern.MATCH_ALL)).thenReturn(assocs);

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The folder is destroyed
		verify(_nodeService, times(0)).deleteNode(folder);
		
		// Double check there wasn't an error (as it doesn't throw an exception)
		verify(_nodeService, times(0)).addAspect(eq(file), eq(ManagedDeletionModel.ASPECT_FAILED_TO_DELETE), any(Map.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteDoesntRemoveParentFolderIfNotMarkedForDelete() {
		
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		
		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		when(_nodeService.exists(file)).thenReturn(true);
		ChildAssociationRef assoc = new ChildAssociationRef(
				ContentModel.ASSOC_CONTAINS, folder, null, file);
		when(_nodeService.getPrimaryParent(file)).thenReturn(assoc);
		
		Path filePath = new Path();
		filePath.append(BASE_TEST_PATH);
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}folder"));
		filePath.append(new PathElement("{http://www.alfresco.org/model/content/1.0}file"));
		
		when(_nodeService.getPath(file)).thenReturn(filePath);
		
		// And all this stuff is done so the method doesn't fail
		NodeRef dummyRef = new NodeRef("store:///x");
		when(_nodeFinder.getNodeRef(any(Path.class), any(SearchCase.class))).thenReturn(dummyRef);
//		when(_nodeFinder.getName(path))

		// And the folder is not marked for delete
		when(
				_nodeService.hasAspect(folder,
						ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION))
				.thenReturn(false);

		// And the file is marked for delete
		when(
				_nodeService.hasAspect(file,
						ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
				.thenReturn(true);

		// And the folder is empty after delete
		when(
				_nodeService.getChildAssocs(folder,
						ContentModel.ASSOC_CONTAINS,
						RegexQNamePattern.MATCH_ALL)).thenReturn(
				new ArrayList<ChildAssociationRef>());

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The folder is destroyed
		verify(_nodeService, times(0)).deleteNode(folder);
		
		// Double check there wasn't an error (as it doesn't throw an exception)
		verify(_nodeService, times(0)).addAspect(eq(file), eq(ManagedDeletionModel.ASPECT_FAILED_TO_DELETE), any(Map.class));
	}
}
