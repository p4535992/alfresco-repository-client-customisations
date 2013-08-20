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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.model.filefolder.FileFolderServiceImpl;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ISO9075;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.surevine.alfresco.model.ManagedDeletionModel;
import com.surevine.alfresco.repo.NodeFinder;
import com.surevine.alfresco.repo.NodeFinder.SearchCase;
import com.surevine.alfresco.repo.delete.ManagedDeletionException;
import com.surevine.alfresco.repo.delete.ManagedDeletionService;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails.ArchivalStatus;
import com.surevine.alfresco.repo.delete.PerishabilityLogic;
import com.surevine.alfresco.repo.delete.SiteNameBasedManagedDeletionService;
import com.surevine.alfresco.repo.delete.SiteNameBasedManagedDeletionService.PathElement;

public class ManagedDeletionServiceTest {

	// Not static or final so we can change it during tests if we want to
	private String currentUserName = "testUser";
	
	private ManagedDeletionService _mds;
	
	FileFolderServiceImpl _fileFolderService;

	@Mock
	BehaviourFilter _behaviourFilter;

	@Mock
	LockService _lockService;

	NodeFinder _nodeFinder;
	
	@Mock
	TransactionService _transactionService;
	
	private NodeService _nodeService;
	
	@Mock
	PerishabilityLogic _perishabilityLogic;
	
	@Mock
	VersionService _versionService;
	
	/**
	 * A realistic looking path used by various test methods herein
	 */
	public static Path DEFAULT_TEST_PATH = new Path();
	static 
	{
		DEFAULT_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		DEFAULT_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		DEFAULT_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble"));
		DEFAULT_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		DEFAULT_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}404page.html"));
	}
	
	/**
	 * A realistic looking path used by various test methods herein
	 */
	public static Path DEFAULT_DELETED_ITEM_TEST_PATH = new Path();
	static 
	{
		DEFAULT_DELETED_ITEM_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		DEFAULT_DELETED_ITEM_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		DEFAULT_DELETED_ITEM_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibbledeletedItems"));
		DEFAULT_DELETED_ITEM_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		DEFAULT_DELETED_ITEM_TEST_PATH.append( new PathElement("{http://www.alfresco.org/model/content/1.0}404page.html"));
	}
		
	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		
		_mds = spy(new SiteNameBasedManagedDeletionService(){
			
			@Override
			protected String getCurrentUserName()
			{
				return currentUserName;
			}

		});
		
		class NullNodeFinder extends NodeFinder {
			@Override
			public NodeRef getNodeRef(Path path, SearchCase caseSensitive)
			{
				return null;
			}
		}

		_nodeFinder = spy(new NullNodeFinder());
		_nodeService = spy(new NodeServiceMock()); //Clear the memory of the nodeservice
		_fileFolderService = spy(new FileFolderServiceImpl());
		
		_fileFolderService.setNodeService(_nodeService);
		
		((SiteNameBasedManagedDeletionService)_mds).setNodeFinder(_nodeFinder);
		((SiteNameBasedManagedDeletionService)_mds).setNodeService(_nodeService);
		((SiteNameBasedManagedDeletionService)_mds).setFileFolderService(_fileFolderService);
		((SiteNameBasedManagedDeletionService)_mds).setPolicyFilter(_behaviourFilter);
		((SiteNameBasedManagedDeletionService)_mds).setLockService(_lockService);
		((SiteNameBasedManagedDeletionService)_mds).setPerishabilityLogic(_perishabilityLogic);
		((SiteNameBasedManagedDeletionService)_mds).setTransactionService(_transactionService);
		((SiteNameBasedManagedDeletionService)_mds).setVersionService(_versionService);
		
		RetryingTransactionHelper rth = new RetryingTransactionHelper() {

			@Override
			public <R> R doInTransaction(RetryingTransactionCallback<R> cb,
					boolean readOnly, boolean requiresNew) {
				try {
					return cb.execute();
				} catch (Throwable e) {
					throw AlfrescoRuntimeException.makeRuntimeException(e, "Exception from transactional callback: " + cb);
				}
			}
			
		};
		
		when(_transactionService.getRetryingTransactionHelper()).thenReturn(rth);
	}
	
	@Test
	public void markForDeleteBasicFlow()
	{
		NodeRef nodeRef =  new NodeRef("store:///1");
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		String s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
	}
	
	@Test
	public void markForDeleteAlreadyMarkedForDelete()
	{
		NodeRef nodeRef =  new NodeRef("store:///1");
		
		//Mark for delete the first time
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		String s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
		
		Date archiveDate = (Date)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME));
		
		//Wait for 2 seconds just to drive the point home about the date not changing (even though we're using == below)
		try {
			Thread.sleep(2000l);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		//Mark the item again - if behavior is correct, this won't do anything and so the archive date won't change
		_mds.markForDelete(nodeRef);
		Date newDate = (Date)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME));
		assertTrue(archiveDate==newDate); //These really should be the same object hence == not .equals
	}
	
	@Test
	public void markForDeleteThenRemoveMark()
	{
		//Mark an item for deletion
		NodeRef nodeRef =  new NodeRef("store:///1");
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		String s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
		
		//Remove the mark from the item and check it's gone
		_mds.removeDeletionMark(nodeRef);
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
	}
	
	@Test
	public void removeMarkFromItemThatNeverHadOne()
	{
		//This is a valid call to make but it shouldn't do anything - so the test passes as long as we don't get an exception
		NodeRef nodeRef =  new NodeRef("store:///1");
		_mds.removeDeletionMark(nodeRef);
	}
	
	@Test
	public void removeMarkFromItemTwice()
	{
		//Mark an item for deletion
		NodeRef nodeRef =  new NodeRef("store:///1");
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		String s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
		
		//Remove the mark from the item and check it's gone
		_mds.removeDeletionMark(nodeRef);
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		
		//Make the call again, primarily to check we don't get an exception but we may as well assert that the aspect hasn't magically leapt back on
		_mds.removeDeletionMark(nodeRef);
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
	}
	
	@Test
	public void addMarkRemoveItThenAddAgainThenRemoveItAgain()
	{
		//Mark an item for deletion
		NodeRef nodeRef =  new NodeRef("store:///1");
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		String s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
		
		//We want to check that the second time we apply the aspect it applies new properties such as date rather than just re-activating the old ones
		Date archiveDate = (Date)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME));
		
		//Wait for 2 seconds just to drive the point home about the date not changing (even though we're using == below)
		try {
			Thread.sleep(2000l);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		//Remove the mark from the item and check it's gone
		_mds.removeDeletionMark(nodeRef);
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		
		//Add the mark back on
		_mds.markForDelete(nodeRef);
		assertTrue(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME).getClass().isAssignableFrom(Date.class));
		assertTrue(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY).getClass().isAssignableFrom(String.class));
		s =(String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY));
		assertTrue(s.equals("testUser"));
		
		//Get the archive date now, which should be after the original date
		Date newArchiveDate = (Date)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME));
		assertTrue(newArchiveDate.after(archiveDate));
		
		//Just to complete the circle, check that we can still remove the aspect
		_mds.removeDeletionMark(nodeRef);
		assertFalse(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
	}
	
	@Test
	public void getDestinationPathBasicFlow()
	{
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble")); //Should get 'deletedItems' added onto it
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}sites")); //Check that documents called "sites" won't get rewritten
		
		Path responsePath = ((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(path);
		assertTrue(responsePath.toString().equals("{http://www.alfresco.org/model/application/1.0}company_home/{http://www.alfresco.org/model/site/1.0}sites/{http://www.alfresco.org/model/content/1.0}wibbledeletedItems/{http://www.alfresco.org/model/content/1.0}documentLibrary/{http://www.alfresco.org/model/content/1.0}sites"));
		
		//If we undelete the response path we should get the original path back
		Path undeletePath = ((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(responsePath, "wibble");
		assertTrue(undeletePath.toString().equals(path.toString()));
	}
	

	@Test (expected = ManagedDeletionException.class)
	public void getDestinationPathFromPathWithNoSitesElement()
	{
		if (!(_mds instanceof SiteNameBasedManagedDeletionService))
		{
			return;
		}
		
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wcmqs"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}404page.html"));
		
		((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(path);
	}
	
	@Test
	public void getDestinationPathWithSpacesIn()
	{
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble sticks"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}ogre bum"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}404page.html"));
		
		Path responsePath = ((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(path);
		assertTrue(responsePath.toString().equals("{http://www.alfresco.org/model/application/1.0}company_home/{http://www.alfresco.org/model/site/1.0}sites/{http://www.alfresco.org/model/content/1.0}wibbledeletedItems/{http://www.alfresco.org/model/content/1.0}documentLibrary/{http://www.alfresco.org/model/content/1.0}wibble sticks/{http://www.alfresco.org/model/content/1.0}ogre bum/{http://www.alfresco.org/model/content/1.0}404page.html"));
		
		//If we undelete the response path we should get the original path back
		Path undeletePath = ((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(responsePath, "wibble");
		assertTrue(undeletePath.toString().equals(path.toString()));
	}
	
	@Test
	public void getDestinationPathWithXMLEscapedSpacesIn()
	{
		class MoreRealisticAlfrescoInternalPathElement extends PathElement
		{
			private String _el;
			public MoreRealisticAlfrescoInternalPathElement(String s)
			{
				super(s);
				_el=s;
			}
			
			@Override
			public String getElementString() {
				String rv = _el.substring(0, _el.lastIndexOf("}")+1)+ISO9075.encode(_el.substring(_el.lastIndexOf("}")+1));
			//	System.out.println(rv);
				return rv;
			}
		}
		
		Path path = new Path();
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/content/1.0}wibble"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/content/1.0}wibble sticks"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/content/1.0}ogre bum"));
		path.append( new MoreRealisticAlfrescoInternalPathElement("{http://www.alfresco.org/model/content/1.0}404page.html"));
		
		Path responsePath = ((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(path);
		System.out.println(responsePath);
		//assertTrue(responsePath.toString().equals("{http://www.alfresco.org/model/application/1.0}company_home/{http://www.alfresco.org/model/site/1.0}sites/{http://www.alfresco.org/model/content/1.0}wibbledeletedItems/{http://www.alfresco.org/model/content/1.0}documentLibrary/{http://www.alfresco.org/model/content/1.0}wibble_x0020_sticks/{http://www.alfresco.org/model/content/1.0}ogre_x0020_bum/{http://www.alfresco.org/model/content/1.0}_x0034_04page.html"));
	}
	
	@Test (expected = ManagedDeletionException.class)
	public void getDestinationPathFromPathWithNothingAfterSitesElement()
	{
		if (!(_mds instanceof SiteNameBasedManagedDeletionService))
		{
			return;
		}
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		
		((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(path);
	}
	
	@Test (expected = ManagedDeletionException.class)
	public void getUndeleteDestinationPathWithNullOriginalSiteName()
	{
		if (!(_mds instanceof SiteNameBasedManagedDeletionService))
		{
			return;
		}
		
		((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(DEFAULT_TEST_PATH, null);
	}
	
	@Test (expected = ManagedDeletionException.class)
	public void getUndeleteDestinationPathWithEmptyOriginalSiteName()
	{
		if (!(_mds instanceof SiteNameBasedManagedDeletionService))
		{
			return;
		}
		
		((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(DEFAULT_TEST_PATH, "   ");
	}
	
	@Test (expected = ManagedDeletionException.class)
	public void getUndeleteDestinationPathButItemIsntInDeletedItemsSite()
	{
		if (!(_mds instanceof SiteNameBasedManagedDeletionService))
		{
			return;
		}
		
		((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(DEFAULT_TEST_PATH, "foo");
	}
	
	@Test
	public void createDestinationFolderStructure()
	{		
		//In order to test this bit of the class we need an unusual level of access, so we perform the traditional trick of emulating c++ in java
		//and poke our friend's privates
		class UnitTestableMDService extends SiteNameBasedManagedDeletionService {
		
		
			public NodeRef createStructure(Path path)
			{
				return createParentFoldersAndGetTargetParent(path);
			}
			
		
			public FileFolderService getFileFolderService()
			{
				return _fileFolderService;
			}
			
			
		};
		
		class UnitTestableNodeFinder extends NodeFinder {
			//Simulate a repo which contains the indicated folders but nothing else
			@Override
			public NodeRef getNodeRef(Path path, SearchCase caseSensitive)
			{
				if (   path.toString().endsWith("company_home")
					|| path.toString().endsWith("sites")
					|| path.toString().endsWith("newsite")
					|| path.toString().endsWith("documentLibrary")
					)
				{
					return new NodeRef("store:///1"); //Any old NodeRef will do, the important thing is it isn't null
				}
				return null;
			}
		}
		
		_mds = new UnitTestableMDService();
		((SiteNameBasedManagedDeletionService)_mds).setNodeFinder(new UnitTestableNodeFinder());
			
		
		((SiteNameBasedManagedDeletionService)_mds).setNodeService(_nodeService);
		((SiteNameBasedManagedDeletionService)_mds).setFileFolderService(new FileFolderServiceMock());
		
		//The actual test starts here - We expect this whole structure after the document library to be created
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsite"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}bar"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}baz"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}quux")); //This represents an item, everything else is a folder
		((UnitTestableMDService)_mds).createStructure(path);
		
		//Get the list of names of items we have created
		Iterator<String> namesCreated = ((FileFolderServiceMock)(((UnitTestableMDService)_mds).getFileFolderService())).names.iterator();
		assertTrue(namesCreated.next().equals("foo"));
		assertTrue(namesCreated.next().equals("bar"));
		assertTrue(namesCreated.next().equals("baz"));
		assertFalse(namesCreated.hasNext());
	}
	
	@Test
	public void getSiteNameFromPath()
	{		
		//In order to test this bit of the class we need an unusual level of access, so we perform the traditional trick of emulating c++ in java
		//and poke our friend's privates
		class UnitTestableMDService extends SiteNameBasedManagedDeletionService {
		
			public String testGetSiteName(NodeRef nodeRef)
			{
				return getSiteName(nodeRef);
			}
			
		};
		
		_mds = new UnitTestableMDService();
		((SiteNameBasedManagedDeletionService)_mds).setNodeService(_nodeService);
		((SiteNameBasedManagedDeletionService)_mds).setFileFolderService(new FileFolderServiceMock());
		
		NodeRef nodeRef =  new NodeRef("store:///1");
		String siteName = ((UnitTestableMDService)_mds).testGetSiteName(nodeRef);
		assertTrue(siteName.equals("wibble"));
	}
	
	// TOOD: There is an issue about encodings that Simon isn't sure about, this needs work.
//	@Test
//	public void pathShortner()
//	{
//		String expectedShortPath="app:company_home/st:sites/cm:wibble/cm:documentLibrary/cm:404page.html";
//		String result = ((SiteNameBasedManagedDeletionService)_mds).getShortPathString(DEFAULT_TEST_PATH);
//		assertTrue(expectedShortPath.equals(result));
//	}
//	
	/**
	 * Self-explanatory, except that for some reason Path doesn't implement .equals(), which is why we're
	 * messing around with .toString()ing them
	 */
	@Test
	public void fileNameCollision()
	{
		//Create a version of the deletion service that allows us to specify a number of files that already
		//exist
		class UnitTestableMDService extends SiteNameBasedManagedDeletionService
		{
			
			private Collection<Path> _paths = new ArrayList<Path>();
			public void addExistingFileAtPath(Path path)
			{
				_paths.add(path);
			}
			
		}
		_mds = new UnitTestableMDService();
	
		((SiteNameBasedManagedDeletionService)_mds).setNodeService(_nodeService);
		((SiteNameBasedManagedDeletionService)_mds).setFileFolderService(new FileFolderServiceMock());
		((SiteNameBasedManagedDeletionService)_mds).setNodeFinder(new NodeFinderMock());

		
		//Add a file that already exists at the destination path
		Path existingItemPath = new Path();
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsitedeletedItems"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo"));
		((UnitTestableMDService)_mds).addExistingFileAtPath(existingItemPath);
		
		//Create the source path - ie. the path of the item we are trying to delete
		Path sourcePath = new Path();
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsite"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo"));
		
		//This is the path we require the service to create
		
		Path expectedPath = new Path();
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsitedeletedItems"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo-1"));
		
		Path destinationPath = ((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(sourcePath);
		
		/*try {
			assertTrue(destinationPath.toString().equals(expectedPath.toString()));
		}
		catch (AssertionFailedError e) {
			System.out.println("***    "+destinationPath.toString());
			throw e;
		}*/
		
		//We're putting multiple assertions in here to remove lots of repeated code
		
		//At this point we've confirmed what happens with a single collision, but what about 100?
		//First, we'll add 99 'foo's to our mock (we've already got foo with no -X in)
		for (int i=1; i<101; i++)
		{
			Path oneOfManyExistingItemsPath = new Path();
			oneOfManyExistingItemsPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
			oneOfManyExistingItemsPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
			oneOfManyExistingItemsPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsitedeletedItems"));
			oneOfManyExistingItemsPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
			oneOfManyExistingItemsPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo-"+i));
			((UnitTestableMDService)_mds).addExistingFileAtPath(oneOfManyExistingItemsPath);
		}
		//We've added 100 items, so we expect foo-101
		expectedPath = new Path();
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsitedeletedItems"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo-101"));
		
		destinationPath = ((SiteNameBasedManagedDeletionService)_mds).getDeleteDesinationPath(sourcePath);
		//assertTrue(destinationPath.toString().equals(expectedPath.toString()));
	}
	
	/**
	 * Also uses undelete instead of delete, just to check that both are working (even though they use the same implementation code)
	 */
	@Test
	public void fileNameCollisionWithExtensionsInFileName()
	{
		//Create a version of the deletion service that allows us to specify a number of files that already
		//exist
		class UnitTestableMDService extends SiteNameBasedManagedDeletionService
		{
			private Collection<Path> _paths = new ArrayList<Path>();
			public void addExistingFileAtPath(Path path)
			{
				_paths.add(path);
			}
		}
		
		class UnitTestableNodeFinder extends NodeFinder {
			//Simulate a repo which contains the indicated folders but nothing else
			@Override
			public NodeRef getNodeRef(Path path, SearchCase caseSensitive)
			{
				if (   path.toString().endsWith("company_home")
					|| path.toString().endsWith("sites")
					|| path.toString().endsWith("newsite")
					|| path.toString().endsWith("documentLibrary")
					)
				{
					return new NodeRef("store:///1"); //Any old NodeRef will do, the important thing is it isn't null
				}
				return null;
			}
		}
		
		_mds = new UnitTestableMDService();
		((SiteNameBasedManagedDeletionService)_mds).setNodeFinder(new UnitTestableNodeFinder());
		((SiteNameBasedManagedDeletionService)_mds).setNodeService(_nodeService);
		((SiteNameBasedManagedDeletionService)_mds).setFileFolderService(new FileFolderServiceMock());

		//Add a file that already exists at the destination path
		Path existingItemPath = new Path();
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsite"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		existingItemPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo.txt.tmp"));
		((UnitTestableMDService)_mds).addExistingFileAtPath(existingItemPath);
		
		//Create the source path - ie. the path of the item we are trying to delete
		Path sourcePath = new Path();
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsitedeletedItems"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		sourcePath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo.txt.tmp"));
		
		//This is the path we require the service to create
		
		Path expectedPath = new Path();
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}newsite"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		expectedPath.append( new PathElement("{http://www.alfresco.org/model/content/1.0}foo-1.txt.tmp"));
		
		Path destinationPath = ((SiteNameBasedManagedDeletionService)_mds).getUndeleteDesinationPath(sourcePath, "newsite");
		
		/**try {
			assertTrue(destinationPath.toString().equals(expectedPath.toString()));
			assertTrue(destinationPath.last().toString().equals("{http://www.alfresco.org/model/content/1.0}foo-1.txt.tmp"));
		}
		catch (AssertionFailedError e) {
			System.out.println("|||   "+destinationPath.toString());
			throw e;
		}**/
	}	
	
	/**
	 * Test the {@link SiteNameBasedManagedDeletionService#getShortPathString(Path)} works correctly and handles spaces
	 */
	@Test
	public void getShortPathStringHandlesSpaces()
	{
		Path path = new Path();
		path.append( new PathElement("{http://www.alfresco.org/model/application/1.0}company_home"));
		path.append( new PathElement("{http://www.alfresco.org/model/site/1.0}sites"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}wibble")); 
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}documentLibrary"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}folder with spaces"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}File with Spaces.doc")); 
	
		String shortPath = ((SiteNameBasedManagedDeletionService)_mds).getNodeFinder().getShortPathString(path);
		
		assertTrue(shortPath.equals("app:company_home/st:sites/cm:wibble/cm:documentLibrary/cm:folder_x0020_with_x0020_spaces/cm:File_x0020_with_x0020_Spaces.doc"));
		
		// Now ensure that it doesn't double encode when the Path is already encoded.
		path = path.subPath(3);
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}folder_x0020_with_x0020_spaces"));
		path.append( new PathElement("{http://www.alfresco.org/model/content/1.0}File_x0020_with_x0020_Spaces.doc")); 
	
		shortPath = ((SiteNameBasedManagedDeletionService)_mds).getNodeFinder().getShortPathString(path);
		assertTrue(shortPath.equals("app:company_home/st:sites/cm:wibble/cm:documentLibrary/cm:folder_x0020_with_x0020_spaces/cm:File_x0020_with_x0020_Spaces.doc"));

	}
	
	@Test
	public void testMarkForDeleteOnFolderSetsAspectAndProperties()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		_mds.markForDelete(folder);

		assertTrue("Required aspect not set", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
		assertEquals("User property not set", currentUserName, _nodeService.getProperty(folder, ManagedDeletionModel.PROP_FOLDER_DELETED_BY));
	}
	
	@Test
	public void testMarkForDeleteOnFolderAlsoMarksContents()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		NodeRef file1 = new NodeRef("store:///2");
		_nodeService.addChild(folder, file1, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef folder2 = new NodeRef("store:///4");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file3 = new NodeRef("store:///5");
		_nodeService.addChild(folder2, file3, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(folder);

		// Check that all the files are marked for deletion
		assertTrue("File1 not marked", _nodeService.hasAspect(file1, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertTrue("File2 not marked", _nodeService.hasAspect(file2, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertTrue("File3 not marked", _nodeService.hasAspect(file3, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertTrue("Folder not marked", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
		assertTrue("Folder2 not marked", _nodeService.hasAspect(folder2, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testMarkForDeleteOnFolderWithSomeItemsAlreadyMarkedDoesntChangeMarkUser()
	{
		String currentUser1 = "User1";
		String currentUser2 = "User2";
		
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		NodeRef file1 = new NodeRef("store:///2");
		_nodeService.addChild(folder, file1, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef folder2 = new NodeRef("store:///4");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file3 = new NodeRef("store:///5");
		_nodeService.addChild(folder2, file3, ContentModel.ASSOC_CONTAINS, null);

		currentUserName = currentUser1;
		
		_mds.markForDelete(file1);
		_mds.markForDelete(folder2);

		currentUserName = currentUser2;
		
		_mds.markForDelete(folder);

		assertEquals("File marked by user overwritten", currentUser1, _nodeService.getProperty(file1, ManagedDeletionModel.PROP_DELETED_BY));
		assertEquals("Unexpected marked by user", currentUser2, _nodeService.getProperty(file2, ManagedDeletionModel.PROP_DELETED_BY));
		assertEquals("Folder marked by user overwritten", currentUser1, _nodeService.getProperty(folder2, ManagedDeletionModel.PROP_FOLDER_DELETED_BY));
		assertEquals("File marked by user overwritten", currentUser1, _nodeService.getProperty(file3, ManagedDeletionModel.PROP_DELETED_BY));
		
		// Check that all the files are still marked for deletion
		assertTrue("File1 not marked", _nodeService.hasAspect(file1, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertTrue("File2 not marked", _nodeService.hasAspect(file2, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertTrue("File3 not marked", _nodeService.hasAspect(file3, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testRemoveFolderDeletionMarkRemovesAspect()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		_mds.markForDelete(folder);
		_mds.removeDeletionMark(folder);

		assertFalse("Required aspect not removed", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testRemoveFolderDeletionMarkAlsoUnmarksContents()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		NodeRef file1 = new NodeRef("store:///2");
		_nodeService.addChild(folder, file1, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef folder2 = new NodeRef("store:///4");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		NodeRef file3 = new NodeRef("store:///5");
		_nodeService.addChild(folder2, file3, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(folder);
		
		_mds.removeDeletionMark(folder);

		// Check that all the files are not marked for deletion
		assertFalse("File1 still marked", _nodeService.hasAspect(file1, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse("File2 still marked", _nodeService.hasAspect(file2, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse("File3 still marked", _nodeService.hasAspect(file3, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testRemoveDeletionMarkOnFileAlsoUnmarksParentFolder()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(folder);

		_mds.removeDeletionMark(file);

		assertFalse("Required aspect not removed", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testRemoveDeletionMarkOnSubfolderAlsoUnmarksParentFolder()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		NodeRef folder2 = new NodeRef("store:///2");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(folder);

		_mds.removeDeletionMark(folder2);

		assertFalse("Required aspect not removed", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testRemoveDeletionMarkOnFolderUnmarksContentsEvenIfFolderIsntMarkedForDelete()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		
		NodeRef folder2 = new NodeRef("store:///3");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(file);
		_mds.markForDelete(folder2);

		_mds.removeDeletionMark(folder);

		assertFalse("Required aspect not removed for item", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION));
		assertFalse("Required aspect not removed for folder", _nodeService.hasAspect(folder2, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testFolderContentsChangedRemovesAspect()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		_mds.markForDelete(folder);
		
		_mds.folderContentsChanged(folder);
		
		assertFalse("Aspect not removed from folder", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testFolderContentsChangedDoesNotAffectContents()
	{
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		
		NodeRef folder2 = new NodeRef("store:///3");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);

		_mds.markForDelete(folder);
		
		_mds.folderContentsChanged(folder);
		
		assertTrue("Aspect unexpectedly removed from file", _nodeService.hasAspect(folder2, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
		assertTrue("Aspect unexpectedly removed from folder", _nodeService.hasAspect(folder2, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testFolderContentsChangedPropapgatesToParents()
	{
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And it contains a subfolder
		NodeRef folder2 = new NodeRef("store:///2");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);
		
		// And both are marked for delete
		_mds.markForDelete(folder);
		
		// +++ When
		// The folderContentsChanged method is called for the subfolder
		_mds.folderContentsChanged(folder2);
		
		// +++ Then
		assertFalse("The aspect should also be removed from the parent folder", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testFolderContentsChangedOnNonMarkedFolderDoesNothing()
	{
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// +++ When
		// The folderContentsChanged method is called for the folder
		_mds.folderContentsChanged(folder);
		
		// +++ Then
		assertFalse("The folder should not have the mds:folderMarkedForDelete aspect", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testDeleteOnFolderContentsDoesNotRemoveFolderDeletionMark()
	{
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		// And it contains a file
		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);

		// And they are both marked for delete
		_mds.markForDelete(folder);
		
		// +++ When
		// The managed deletion of the file takes place
		_mds.delete(file);
		
		// +++ Then
		assertTrue("The folder should still have the md:folderMarkedForDeletion aspect", _nodeService.hasAspect(folder, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION));
	}
	
	@Test
	public void testDeleteDisablesFolderAspectBehavioursWhileDeleting() throws Exception {
		// We don't really care about the paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		

		// +++ Given
		// We have a file
		NodeRef file = new NodeRef("store:///2");

		// And the file is marked for delete
		_mds.markForDelete(file);

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The FolderMarkedForDeletion aspect behaviours are disabled
		verify(_behaviourFilter).disableBehaviour(
				ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
		
		assertFalse("The delete operation shouldn't fail", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE));
	}

	@Test
	@Ignore
	public void testDeleteRemovesParentFolderIfMarkedForDeleteAndEmptyAfterDelete() throws Exception {
		// We don't really care about the paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
//		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		

		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);
		
		// And it contains a file
		NodeRef file = new NodeRef("store:///2");
		_nodeService.setType(folder, ContentModel.TYPE_CONTENT);
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);

		// And the folder is marked for delete
		_mds.markForDelete(folder);

		// +++ When
		// We delete the file
		_mds.delete(file);

		// +++ Then
		// The folder is destroyed
		verify(_nodeService).deleteNode(folder);
		
		assertFalse("The delete operation shouldn't fail", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE));
	}

	@SuppressWarnings({ "serial" })
	@Test
	public void testUndeleteOnFolderUndeletesContents() throws Exception {
		// We don't really care about the undelete paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		
		
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file);
		
		// And another file inside the folder
		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file2);
		
		// And both the files are deleted
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName,Serializable>() {{
			put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibble");
		}});
		_nodeService.addAspect(file2, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName,Serializable>() {{
			put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibble");
		}});
		
		// +++ When
		// We undelete the folder
		_mds.undelete(folder);
		
		// +++ Then
		// Both files should be undeleted
		verify(_mds).undelete(file);
		verify(_mds).undelete(file2);
	}

	@Test
	public void testUndeleteOnFolderUndeletesSubfolder() throws Exception {
		// We don't really care about the undelete paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		
		
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And a subfolder inside the folder
		NodeRef folder2 = new NodeRef("store:///2");
		_nodeService.setType(folder2, ContentModel.TYPE_FOLDER);
		_nodeService.addChild(folder, folder2, ContentModel.ASSOC_CONTAINS, null);
		
		// +++ When
		// We undelete the folder
		_mds.undelete(folder);
		
		// +++ Then
		// The subfolder file should be undeleted
		verify(_mds).undelete(folder2);
	}
	
	@SuppressWarnings({ "serial" })
	@Test
	public void testUndeleteOnFolderDestroysFolderOnSuccess() throws Exception {
		// We don't really care about the undelete paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		

		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file);
		
		// And another file inside the folder
		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file2);
		
		// And both the files are deleted
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName, Serializable>() {{
			this.put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibbledeletedItems");
		}});
		_nodeService.addAspect(file2, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName, Serializable>() {{
			this.put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibbledeletedItems");
		}});
		
		// +++ When
		// We undelete the folder
		_mds.undelete(folder);
		
		// +++ Then
		// The folder should be destroyed
		verify(_nodeService).deleteNode(folder);
	}
	
	@SuppressWarnings({ "serial" })
	@Test
	public void testUndeleteOnFolderDoesntDestroyFolderOnFailure() throws Exception {
		// We don't really care about the undelete paths - we just want it to run through
		doReturn(new NodeRef("store:///123")).when(_nodeFinder).getNodeRef(any(Path.class), any(SearchCase.class));
		doReturn(mock(FileInfo.class)).when(_fileFolderService).move(any(NodeRef.class), any(NodeRef.class), anyString());		

		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file);
		
		// And another file inside the folder
		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_DELETED_ITEM_TEST_PATH).when(_nodeService).getPath(file2);
		
		// And both the files are deleted
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName, Serializable>() {{
			this.put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibbledeletedItems");
		}});
		_nodeService.addAspect(file2, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName, Serializable>() {{
			this.put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, "wibbledeletedItems");
		}});
		
		// And trying to move one will cause an exception
		doThrow(FileNotFoundException.class).when(_fileFolderService).move(eq(file2), any(NodeRef.class), anyString());
		
		// +++ When
		// We undelete the folder
		boolean exceptionCaught = false;
		
		try {
			_mds.undelete(folder);
		} catch(ManagedDeletionException eMD) {
			exceptionCaught = true;
		}
		
		// +++ Then
		assertTrue("A ManagedDeletionException should be thrown", exceptionCaught);
		
		// The folder should not be destroyed
		verify(_nodeService, times(0)).deleteNode(folder);
	}
	
	@Test
	public void testDeleteOnFolderAlsoDeletesContents() {
		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		_nodeService.setType(folder, ContentModel.TYPE_FOLDER);

		// And a file inside the folder
		NodeRef file = new NodeRef("store:///2");
		_nodeService.addChild(folder, file, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_TEST_PATH).when(_nodeService).getPath(file);
		
		// And another file inside the folder
		NodeRef file2 = new NodeRef("store:///3");
		_nodeService.addChild(folder, file2, ContentModel.ASSOC_CONTAINS, null);
		doReturn(DEFAULT_TEST_PATH).when(_nodeService).getPath(file2);
		
		// When we delete the folder
		_mds.delete(folder);
		
		// Then
		// The files have been deleted
		verify(_mds).delete(file);
		verify(_mds).delete(file2);
	}
	
	@Test
	public void testSetPerishable() {
		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
		
		// +++ When
		// We mark the item as perishable
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		_mds.setPerishable(file, perishReason);
		
		// +++ Then
		assertTrue("The file has the perishable aspect", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
		assertEquals("The reason code is as expected", perishReason, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REASON));
		assertEquals("The perish user is correct", currentUserName, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REQUESTED_BY));
		assertEquals("The perish due date is correct", perishDue, _nodeService.getProperty(file,  ManagedDeletionModel.PROP_PERISH_DUE));
	}
	
	@Test
	public void testSetPerishableOnTopic() {
		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ForumModel.TYPE_TOPIC);
		
		// +++ When
		// We mark the item as perishable
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		_mds.setPerishable(file, perishReason);
		
		// +++ Then
		assertTrue("The file has the perishable aspect", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
		assertEquals("The reason code is as expected", perishReason, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REASON));
		assertEquals("The perish user is correct", currentUserName, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REQUESTED_BY));
		assertEquals("The perish due date is correct", perishDue, _nodeService.getProperty(file,  ManagedDeletionModel.PROP_PERISH_DUE));
	}
	
	@Test // Need the requirements around this tightened up
	public void testSetPerishableAlreadyPerishable() {
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		String perishReason2 = "reasonCode2";		
		Date perishDue2 = new Date(new Date().getTime() + 10000000);
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason2), any(Date.class))).thenReturn(perishDue2);

		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
		
		// And the item is already perishable
		_mds.setPerishable(file, perishReason);

		// +++ When
		// We mark the item as perishable for a different reason as a different user
		String oldUserName = currentUserName;
		currentUserName = "aDifferentUser";
		_mds.setPerishable(file, perishReason2);
		
		// +++ Then
		// The perish properties are not updated
		assertTrue("The file still has the perishable aspect", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
		assertEquals("The reason code has not been updated", perishReason, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REASON));
		assertEquals("The perish user has not been updated", oldUserName, _nodeService.getProperty(file, ManagedDeletionModel.PROP_PERISH_REQUESTED_BY));
		assertEquals("The perish due date has not been updated", perishDue, _nodeService.getProperty(file,  ManagedDeletionModel.PROP_PERISH_DUE));
	}
	
	@Test
	public void testSetPerishableRemovePerishMark() {
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
		
		// And the item is already perishable
		_mds.setPerishable(file, perishReason);
		
		// +++ When
		// We remove the perish mark
		_mds.setPerishable(file, null);
		
		// +++ Then
		assertFalse("The file doesn't have the perishable aspect", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
	}
	
	@Test
	public void testSetPerishableRemovePerishMarkButNotPerishable() {

		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
		
		// +++ When
		// We remove the perish mark
		_mds.setPerishable(file, null);
		
		// +++ Then
		assertFalse("The file doesn't have the perishable aspect", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=IllegalArgumentException.class)
	public void testSetPerishableInvalidReason() {
		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
		
		// +++ When
		// We set it perishable with an invalid reason code
		String perishReason = "reasonCode";		
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenThrow(IllegalArgumentException.class);
		_mds.setPerishable(file, perishReason);
		
		// +++ Then an exception is thrown
	}
	
	@Test(expected=ManagedDeletionException.class)
	public void testSetPerishableOnDeletedItem() {
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		when(_nodeService.getType(file)).thenReturn(ContentModel.TYPE_CONTENT);
	
		// And the item is deleted
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName,Serializable>());

		// +++ When
		// We mark the item as perishable
		_mds.setPerishable(file, perishReason);
		
		// +++ Then
		// An exception is thrown
	}
	
	@Test(expected=ManagedDeletionException.class)
	public void testSetPerishableOnFolder() {
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		// +++ Given
		// We have a folder
		NodeRef folder = new NodeRef("store:///1");
		when(_nodeService.getType(folder)).thenReturn(ContentModel.TYPE_FOLDER);

		// +++ When
		// We mark the item as perishable
		_mds.setPerishable(folder, perishReason);
		
		// +++ Then
		// An exception is thrown
	}
	
	@Test
	public void testGetArchivalDetailsUnmarked() {
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.UNMARKED, null, null, null);
		assertEquals("The details should be UNMARKED", expected, details);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsMarkedForDelete() {
		final Date testDate = new Date();
		final String testUser = "testuser";
		
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");
		
		// And it's marked for delete
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_DELETED_BY, testUser);
			put(ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME, testDate);
		}});

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.MARKED_FOR_DELETE, testDate, testUser, null);
		assertEquals("The details should be MARKED_FOR_DELETE", expected, details);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsMarkedPerishable() {
		final Date testDate = new Date();
		final String testUser = "testuser";
		final String testPerishReason = "perishreason";
		
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");
		
		// And it's marked perishable
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_PERISH_REQUESTED_BY, testUser);
			put(ManagedDeletionModel.PROP_PERISH_DUE, testDate);
			put(ManagedDeletionModel.PROP_PERISH_REASON, testPerishReason);
		}});

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.PERISHABLE, testDate, testUser, testPerishReason);
		assertEquals("The details should be PERISHABLE", expected, details);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsMarkedPerishableAndForDeletePerishFirst() {
		final Date perishDate = new Date(123456789);
		final String perishUser = "testuser";
		final String perishReason = "perishreason";

		final Date deleteDate = new Date(234567890);	// Note this is *after* the perish due
		final String deleteUser = "testdeleteuser";
		
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");
		
		// And it's marked perishable
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_PERISH_REQUESTED_BY, perishUser);
			put(ManagedDeletionModel.PROP_PERISH_DUE, perishDate);
			put(ManagedDeletionModel.PROP_PERISH_REASON, perishReason);
		}});
		
		// And it's marked for delete
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_DELETED_BY, deleteUser);
			put(ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME, deleteDate);
		}});

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.PERISHABLE, perishDate, perishUser, perishReason);
		assertEquals("The details should be PERISHABLE", expected, details);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsMarkedPerishableAndForDeleteDeleteFirst() {
		final Date perishDate = new Date(123456789);
		final String perishUser = "testuser";
		final String perishReason = "perishreason";

		final Date deleteDate = new Date(12345678);	// Note this is *before* the perish due
		final String deleteUser = "testdeleteuser";
		
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");
		
		// And it's marked perishable
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_PERISH_REQUESTED_BY, perishUser);
			put(ManagedDeletionModel.PROP_PERISH_DUE, perishDate);
			put(ManagedDeletionModel.PROP_PERISH_REASON, perishReason);
		}});
		
		// And it's marked for delete
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_DELETED_BY, deleteUser);
			put(ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME, deleteDate);
		}});

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.MARKED_FOR_DELETE, deleteDate, deleteUser, null);
		assertEquals("The details should be MARKED_FOR_DELETE", expected, details);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsDeleted() {
		final Date testDate = new Date(123456789);
		
		// +++ Given
		// We have an item
		NodeRef file = new NodeRef("store:///1");
		
		// And it's deleted
		_nodeService.addAspect(file, ManagedDeletionModel.ASPECT_DELETED, new HashMap<QName, Serializable>() {{
			put(ManagedDeletionModel.PROP_DELETED_TIMESTAMP, testDate);
		}});

		// +++ When
		// We get the archive details
		NodeArchivalDetails details = _mds.getArchivalDetails(file);
		
		// +++ Then
		NodeArchivalDetails expected = new NodeArchivalDetails(ArchivalStatus.DELETED, null, null, null);
		assertEquals("The details should be DELETED", expected, details);
	}
	
	@Ignore
	@Test
	public void testDeleteOnPerishableItem() {
		// +++ Given
		// We have a document
		NodeRef file = new NodeRef("store:///1");
		
		// And the item is perishable
		String perishReason = "reasonCode";		
		Date perishDue = new Date();
		when(_perishabilityLogic.calculatePerishDue(eq(perishReason), any(Date.class))).thenReturn(perishDue);

		_mds.setPerishable(file, perishReason);
		
		// +++ When
		// When the item is deleted
		_mds.delete(file);
		
		// +++ Then
		assertFalse("The delete operation shouldn't fail: " + _nodeService.getProperty(file, ManagedDeletionModel.PROP_DELETE_FAILURE_MESSAGE), _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE));
		assertTrue("The node should now be deleted", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_DELETED));
		assertFalse("The node should no longer be perishable", _nodeService.hasAspect(file, ManagedDeletionModel.ASPECT_PERISHABLE));
	}
	
	@Test
	public void testDestroyVersionedContent() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "1");
		
		// That is versioned
		when(_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)).thenReturn(true);
		
		// +++ When
		// We destroy the item
		_mds.destroy(nodeRef);
		
		// +++ Then
		InOrder inOrder = Mockito.inOrder(_nodeService, _versionService);
		
		// The node is set as temporary
		inOrder.verify(_nodeService).addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
		
		// and all versions are deleted
		inOrder.verify(_versionService).deleteVersionHistory(nodeRef);
		inOrder.verify(_nodeService).removeAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE);
		
		// and then deleted
		inOrder.verify(_nodeService).deleteNode(nodeRef);
	}
	
	@Test
	public void testDestroyUnversionedContent() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "1");
		
		// That is not versioned
		when(_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)).thenReturn(false);
		
		// +++ When
		// We destroy the item
		_mds.destroy(nodeRef);
		
		// +++ Then
		InOrder inOrder = Mockito.inOrder(_nodeService);
		
		// The node is set as temporary
		inOrder.verify(_nodeService).addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
		
		// and then deleted
		inOrder.verify(_nodeService).deleteNode(nodeRef);
	}
}
