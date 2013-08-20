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


import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.lock.UnableToAquireLockException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.Element;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ISO9075;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.model.ManagedDeletionModel;
import com.surevine.alfresco.repo.NodeFinder;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails.ArchivalStatus;

/**
 * Implementation of ManagedDeletionService that uses graveyard site to manage deletions, and links graveyard sites with normal sites by means
 * of applying a constant postfix to the normal sites name
 * @author simonw
 *
 */
public class SiteNameBasedManagedDeletionService implements ManagedDeletionService {

	/**
	 * This really is only a theoretical limit to stop us hitting infinite loops due to some logic error
	 * and so isn't configurable
	 **/
	private static final int MAX_PATH_DEDUPE_LIMIT=10000;
	
	private static final Log _logger = LogFactory.getLog(SiteNameBasedManagedDeletionService.class);
			
	private NodeFinder _nodeFinder; //Injected
	public void setNodeFinder(NodeFinder nf) {
		_nodeFinder=nf;
	}
	public NodeFinder getNodeFinder() { //Included mainly for testing purposes
		return _nodeFinder;
	}
	
	private LockService _lockService;
	public void setLockService(LockService lockService)
	{
		_lockService=lockService;
	}
	
	private BehaviourFilter _policyFilter;
	public void setPolicyFilter(BehaviourFilter setFilter)
	{
		_policyFilter=setFilter;
	}
	
	/**
	 * Injected
	 */
	private NodeService _nodeService;
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	protected FileFolderService _fileFolderService; //Protected for use in anon subclass in unit test
	public void setFileFolderService(FileFolderService fileFolderService)
	{
		_fileFolderService=fileFolderService;
	}
	
	private PerishabilityLogic _perishabilityLogic;
	public void setPerishabilityLogic(PerishabilityLogic perishabilityLogic)
	{
		_perishabilityLogic = perishabilityLogic;
	}
	
	private TransactionService _transactionService;
	public void setTransactionService(TransactionService transactionService)
	{
		_transactionService = transactionService;
	}
	
	private VersionService _versionService;
	public void setVersionService(VersionService versionService)
	{
		_versionService = versionService;
	}
	
	/**
	 * What is the shortest amount of time that an item should remain in the marked-for-delete state before being archived into a graveyard site automatically?
	 * Note that the value in spring config (and passed into this method) is seconds but the value stored in the field in this class is in milliseconds.  
	 * Should be injected, but defaults to one minute for testing purposes
	 * @param seconds
	 */
	public void setDeletionWindowInSeconds(long seconds)
	{
		_deletionWindowInMillis=seconds*1000l;
	}
	private long _deletionWindowInMillis=60000l;
	
	/**
	 * What should we add onto the name of a regular site to get the name of the equivalent deleted items site.  Injectable, but defaults to "deletedItems"
	 * @param postfix
	 */
	public void setDeletedItemsSiteNamePostfix(String postfix)
	{
		_deletedItemsSiteNamePostfix=postfix;
	}
	private String _deletedItemsSiteNamePostfix="deletedItems";
	
	
	

	
	/**
	 * Mark the given NodeRef for deletion.  This applies a "markedForDeletion" aspect to the item, and sets properties indicating when the item should
	 * be deleted, and who marked the item for deletion.
	 * <p>If the {@link NodeRef} is a folder then {@link #markForDelete(NodeRef)} is called recursively for each item within
	 * the folder.
	 */
	@Override
	public void markForDelete(NodeRef nodeRef) {
		if (_logger.isInfoEnabled())
		{
			_logger.info("Marking "+nodeRef+" for deletion");
		}
		
		String deleterUserName = getCurrentUserName();

		QName nodeType = _nodeService.getType(nodeRef);
		
		if(ContentModel.TYPE_FOLDER.equals(nodeType)) {
			if(_logger.isDebugEnabled()) {
				_logger.debug("Folder " + nodeRef + " is being marked for delete");
			}
			
			List<ChildAssociationRef> children = _nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS,
					RegexQNamePattern.MATCH_ALL);
			
			for(ChildAssociationRef assoc : children) {
				markForDelete(assoc.getChildRef());
			}
			
			if(!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION)) {
				Map<QName, Serializable> properties = new HashMap<QName, Serializable>(2);
				properties.put(ManagedDeletionModel.PROP_FOLDER_DELETED_BY, deleterUserName);
				
				_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION, properties);
			} else if (_logger.isDebugEnabled()) {
				_logger.debug("Folder " + nodeRef + " already has the folderMarkedForDelete aspect");
			}
		} else {
			if (_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
			{
				_logger.warn(nodeRef+" has already been marked for deletion.  Aborting");
				return;
			}
			
			// If this node has previously failed to delete, remove records of that failure
			if (_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE))
			{
				_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE);
			}
			
			Date expirationDate = new Date(new Date().getTime()+_deletionWindowInMillis);
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Marking deletion of "+nodeRef+" with archive date "+expirationDate+" from user "+deleterUserName);
			}
			
			Map<QName, Serializable> properties = new HashMap<QName, Serializable>(2);
			properties.put(ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME, expirationDate);
			properties.put(ManagedDeletionModel.PROP_DELETED_BY, deleterUserName);
	
			_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION, properties);
		}
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug(nodeRef+" succesfully marked for deletion");
		}
	}
	
	/**
	 * Remove a deletion mark from the given NodeRef.
	 * <p>If the NodeRef is a folder, then we also unmark the contents for delete
	 * <p>If the NodeRef wasn't marked for delete, we simply log and do nothing.
	 * In order to remove the deletion mark, we simply remove the aspect, which will automatically remove the relevant properties
	 */
	@Override
	public void removeDeletionMark(NodeRef nodeRef)
	{
		if (_logger.isInfoEnabled())
		{
			_logger.info("Removing deletion mark from "+nodeRef);
		}
		
		QName nodeType = _nodeService.getType(nodeRef);
		
		if(ContentModel.TYPE_FOLDER.equals(nodeType)) {
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Removing deletion mark from folder contents");
			}
			
			List<ChildAssociationRef> children = _nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS,
					RegexQNamePattern.MATCH_ALL);
			
			for(ChildAssociationRef assoc : children) {
				removeDeletionMark(assoc.getChildRef());
			}
			
			if(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION)) {
				_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
			} else {
				if(_logger.isInfoEnabled()) {
					_logger.info("Folder " + nodeRef + " was not marked for deletion");
				}
			}
		} else {
			if (!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION))
			{
				_logger.warn(nodeRef+" was not marked for deletion, so could not remove the deletion mark");
			}
			else
			{
				_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION);
			}
		}
		
		// Finally we will remove all the folder deletion marks for all the parent folders (until we reach one which isn't
		// marked for delete).
		ChildAssociationRef parentAssoc;
		NodeRef currentNodeRef = nodeRef;
		
		while((parentAssoc = _nodeService.getPrimaryParent(currentNodeRef)) != null) {
			currentNodeRef = parentAssoc.getParentRef();
			
			if(!_nodeService.hasAspect(currentNodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION)) {
				break;
			}
			
			_nodeService.removeAspect(currentNodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
		}
	}
	
	/**
	 * Delete the item.  This moves the item to the deleted items site, removed the marked-for-delete aspect and adds a deleted aspect
	 * @param nodeRef NodeRef of the item to delete
	 * @return the NodeRef of the new parent node, or <code>null</code> if deleting a folder.
	 */
	@Override
	public NodeRef delete(final NodeRef nodeRef) 
	{
		QName nodeType = _nodeService.getType(nodeRef);
		
		if(ContentModel.TYPE_FOLDER.equals(nodeType)) {
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Deleting the contents of folder " + nodeRef);
			}
			
			List<ChildAssociationRef> children = _nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS,
					RegexQNamePattern.MATCH_ALL);
			
			for(ChildAssociationRef assoc : children) {
				delete(assoc.getChildRef());
			}
			
			return null;
		}
		
		try {
			//Double check the locking, as am unsure of exact semantics of .lock
			try 
			{
				_lockService.checkForLock(nodeRef);
			}
			catch (NodeLockedException nle)
			{
				_logger.warn("Could not delete "+nodeRef+" as the node was locked");
				throw new ManagedDeletionLockException(nodeRef, "Could not delete "+nodeRef+" as the node was locked", nle);
			}
			
			try
			{
				_lockService.lock(nodeRef, LockType.WRITE_LOCK);
			}
			catch (UnableToAquireLockException uale)
			{
				_logger.warn("Could not delete "+nodeRef+" as a lock could not be acquired");
				throw new ManagedDeletionLockException(nodeRef, "Could not delete "+nodeRef+" as a lock could not be acquired", uale);
			}
			
			final NodeRef[] targetWrapper = new NodeRef[1];
			
			try {
				try {
					_transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Boolean>() {
						@Override
						public Boolean execute() throws Throwable {
							_logger.debug("Attempting delete of " + nodeRef);
				
							//We should only be able to delete things that have been marked as Delete or Perishable
							if (!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION)
									&& !_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE))
							{
								throw new ManagedDeletionException(nodeRef,
										"Item is not marked for deletion or perishable, so cannot be deleted. This may simply be because it has already been deleted.");
							}
							
							//If this node has previously failed to delete, remove records of that failure
							if (_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE))
							{
								_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE);
							}
							
							//Disable audit policy to maintain metadata and stop hitting bugs when running from quartz
							//This call only applies to the current transaction and so all policies will re-enable upon commit
							//and this code is thread-safe
							_policyFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
							
							// Disable the mds:folderMarkedForDelete behaviours so that the parent folder doesn't have its deletion
							// mark removed (if it has one)
							_policyFilter.disableBehaviour(ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
							
							//Work out where we will be moving the item to
							Path destinationPath = getDeleteDesinationPath(_nodeService.getPath(nodeRef));
							if (_logger.isDebugEnabled())
							{
								_logger.debug("Moving item to: "+destinationPath);
							}
							
							// Include a property that indicates the original site the item was deleted.
							String originalSiteName = getSiteName(nodeRef);
							Map<QName, Serializable> propertiesMap = new HashMap<QName, Serializable>();
							propertiesMap.put(ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME, originalSiteName);
							propertiesMap.put(ManagedDeletionModel.PROP_DELETED_TIMESTAMP, new Date());
							
							if (_logger.isDebugEnabled())
							{
								_logger.debug("Original site name: "+originalSiteName);
							}
				
							// Get the current parent folder before we move the file (we will use this later)
							ChildAssociationRef parentAssoc = _nodeService.getPrimaryParent(nodeRef);
			
							// Move the item to it's destination.
							final NodeRef target = moveNode(nodeRef, destinationPath);
							
							//Add the deleted aspect to the node, and remove the mark for deletion and perishable aspects.
							_nodeService.addAspect(target, ManagedDeletionModel.ASPECT_DELETED, propertiesMap);
							_nodeService.removeAspect(target, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION);
							_nodeService.removeAspect(target, ManagedDeletionModel.ASPECT_PERISHABLE);
							
							// Check if the parent folder is marked for delete, and whether it is now empty
							if(parentAssoc != null) {
								NodeRef parentNodeRef = parentAssoc.getParentRef();
								
								if(_nodeService.hasAspect(parentNodeRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION)) {
									List<ChildAssociationRef> children = _nodeService.getChildAssocs(parentNodeRef,
											ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
									
									if(children.isEmpty()) {
										_nodeService.deleteNode(parentNodeRef);
									}
								}
							}
							
							if (_logger.isInfoEnabled())
							{
								_logger.info("NodeRef "+nodeRef+" has been succesfully deleted");
							}
							
							targetWrapper[0] = target;
							
							return Boolean.TRUE;
						}
					}, false, false);
				} catch(AlfrescoRuntimeException e) {
					_logger.error("An error was encountered while attempting to delete " + nodeRef, e);
					
					throw e;
				}
			}
			/**
			 * If we get an exception, attempt to record the exception in the repository against the item
			 * (this will stop the quartz job from trying to delete the item again, but the UI will still allow for future attempts).
			 * If we can't for whatever reason, then just throw the error
			 */
			catch (Exception e)
			{
				//If the node doesn't exist anymore, don't do any special recording and just re-throw
				if (_nodeService.exists(nodeRef))
				{
					Map<QName, Serializable> propertiesMap = new HashMap<QName, Serializable>();
					propertiesMap.put(ManagedDeletionModel.PROP_DELETE_FAILURE_DATE, new Date());
					propertiesMap.put(ManagedDeletionModel.PROP_DELETE_FAILURE_MESSAGE, e instanceof AlfrescoRuntimeException ? e.getCause().toString() : e.toString());
					_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE, propertiesMap);
				}
				boolean failedToPerish = false;
				
				if(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_PERISH_REASON) != null) {
					Date perishDue = (Date) _nodeService.getProperty(nodeRef,ManagedDeletionModel.PROP_PERISH_DUE);
					
					if(perishDue.compareTo(new Date()) < 0) {
						failedToPerish = true;
					}
				}
				
				// Raise a different error message if the node has failed to be perished
				if(failedToPerish) {
					_logger.error("Failed to perish " +nodeRef, e);
				} else {
					_logger.error("Failed to delete " +nodeRef, e);
				}
			}
			
			return targetWrapper[0];
		}
		//If we locked the node during this process, then try to unlock it when we exit, bearing in mind the node may no longer exist
		finally 
		{
			if (_nodeService.exists(nodeRef))
			{
				try 
				{
					_lockService.unlock(nodeRef);
				}
				catch (Exception e)
				{
					throw new ManagedDeletionLockException(nodeRef, "After attempting to delete the node "+nodeRef+", the nodeRef still existed, and could not be unlocked due to a: "+e, e);
				}
			}
		}
	}
	
	/**
	 * Get the name of the site containing the given NodeRef
	 * @param nodeRef
	 * @return The name of the site.  If the NodeRef is not in a site, then behaviour is undefined
	 * extracts FOO from a string like this: /asasas/site/{namespace}FOO/dffddfr
	 */
	protected String getSiteName(NodeRef nodeRef)
	{
		Path path = _nodeService.getPath(nodeRef);
		String pathStr = path.toString();
		String afterSitesPart = pathStr.substring(pathStr.indexOf("sites/")+6); //6 = length of "sites/"
		String afterNameSpace = afterSitesPart.substring(afterSitesPart.indexOf("}")+1);
		return afterNameSpace.substring(0, afterNameSpace.indexOf("/"));
	}

	
	/**
	 * Given a path to a target item (that may or may not exist yet), return the parent of the target, creating as folders any missing parent items along the way
	 * @param target
	 * @return
	 */
	protected NodeRef createParentFoldersAndGetTargetParent(Path target)
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Creating parent folders for "+target);
		}
		//Keep moving up the path, except for the last element, creating any missing elements as folders
		Iterator<Element> elements = target.iterator();
		
		Path totalPath= new Path(); //Build up the path gradually, adding elements that aren't there just like <code>mkdir -p</code>
		
		while (elements.hasNext())
		{
			Element el = elements.next();
			String elStr = el.getElementString();
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Element: "+elStr);
			}
			
			totalPath.append(el);
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("NodeRef for:" +totalPath+" == "+_nodeFinder.getNodeRef(totalPath, NodeFinder.SearchCase.CASE_INSENSITIVE));
			}

			if (_nodeFinder.getNodeRef(totalPath, NodeFinder.SearchCase.CASE_INSENSITIVE)==null && elements.hasNext()) //If the path doesn't exist (and there's at least one more element => folder), which will be because the appended element doesn't exist yet
			{
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Creating a folder at: "+totalPath.toString()+" within: "+totalPath.subPath(totalPath.size()-2));  	//The -2 gets us the last-but one element - in other
																																	 	//words, the folder the target item is in rather than
					     																												//the item itself
				}
				//-1 would be the last element, but as we want the parent of the last element we use -2.  the } bit removes the namespace from the String
				
				final NodeRef parentNodeRef = _nodeFinder.getNodeRef(totalPath.subPath(totalPath.size()-2), NodeFinder.SearchCase.CASE_INSENSITIVE);
				final String decodedName = ISO9075.decode(elStr.substring(elStr.indexOf("}")+1));
				try {
					_fileFolderService.create(parentNodeRef, decodedName, ContentModel.TYPE_FOLDER);
				} catch (final FileExistsException e) {
					_logger.warn(String.format("Failed to create parent folder for %s as it already exists.", target.toString()));
				}
			}
		}
		
		NodeRef rVal = _nodeFinder.getNodeRef(totalPath.subPath(totalPath.size()-2), NodeFinder.SearchCase.CASE_INSENSITIVE);
		
		if (_logger.isInfoEnabled())
		{
			_logger.info("Created path for destination node at: "+totalPath+" with NodeRef: "+rVal);
		}
		return rVal;
	}
	
	/**
	 * Given the path of an item to delete, return the path of it's destination inside a deleted items site
	 * @param originalPath Path of an item to delete, which is located inside exactly one site
	 * @return Path equivalent to the input path, but with the normal site replaced by the deleted items site
	 */
	public Path getDeleteDesinationPath(Path originalPath)
	{
		return getDesinationPathImpl(originalPath, true, null);
	}
	
	/**
	 * Given the path of an item to delete, return the path of it's destination inside a deleted items site
	 * @param originalPath Path of an item to delete, which is located inside exactly one site
	 * @return Path equivalent to the input path, but with the normal site replaced by the deleted items site
	 */
	public Path getUndeleteDesinationPath(Path originalPath, String originalSiteName)
	{
		return getDesinationPathImpl(originalPath, false, originalSiteName);
	}
	
	/**
	 * Path builder method used to construct destination paths for deleting and undeleting items
	 * @param originalPath The path of the item in it's current state
	 * @param delete If true, proceed to create a destination path for deleting the item.  If false, undelete
	 * @param originalSite Only valid when <code>delete==false</code>, the name of the original site to move the item back into
	 * @return
	 */
	private Path getDesinationPathImpl(Path originalPath, boolean delete, String originalSite)
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Generating destination path from "+originalPath+" with delete: "+delete+" and original site: "+originalSite);
		}
		
		//Copy all the path elements, except the ones just after st:sites which we rename slightly
		Path destination = new Path();
		Iterator<Element> currentPathElements = originalPath.iterator();
		boolean rewriteNextElement=false;
		boolean rewritten=false;
		
		//Go through each element in the path.  When we find the st:sites element, we know the next element is the site name, which we alter to point
		//to the deleted items site.  All other path elements are left unmolested
		while (currentPathElements.hasNext())
		{
			Element element = currentPathElements.next();
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Processing path element: "+  element.getElementString());
			}
			if (rewriteNextElement)
			{
				String elementStr =element.getElementString();
				
				if (delete)
				{
					if (elementStr.endsWith(_deletedItemsSiteNamePostfix))
					{
						throw new ManagedDeletionException(_nodeFinder.getNodeRef(originalPath, NodeFinder.SearchCase.CASE_SENSITIVE), "Cannot delete a node which is already in a deleted items site");
					}
					destination.append(new PathElement(elementStr.concat(_deletedItemsSiteNamePostfix)));
				}
				else
				{
					if (!elementStr.endsWith(_deletedItemsSiteNamePostfix))
					{
						throw new ManagedDeletionException(_nodeFinder.getNodeRef(originalPath, NodeFinder.SearchCase.CASE_SENSITIVE), "Cannot undelete a node which is not in a deleted items site");
					}
					if (originalSite==null || originalSite.trim().length()<1)
					{
						throw new ManagedDeletionException(_nodeFinder.getNodeRef(originalPath, NodeFinder.SearchCase.CASE_SENSITIVE), "Cannot undelete a node which has an empty or null original site");
					}
					destination.append(new PathElement("{http://www.alfresco.org/model/content/1.0}"+originalSite));
				}
				rewriteNextElement=false;
				rewritten=true;
			}
			else
			{
				destination.append(new PathElement(ISO9075.decode(element.getElementString())));
				if (element.getElementString().equals("{http://www.alfresco.org/model/site/1.0}sites"))
				{
					rewriteNextElement=true;
				}
			}
		}
		
		//If we failed to re-write anything, which is probably because the original item wasn't in a site, then throw an exception
		if (!rewritten)
		{
			throw new ManagedDeletionException(new NodeRef("workspace://SpacesStore/"+originalPath.toString()), "Path could not be rewritten.  The node may not have been in a site to begin with");
		}
		
		//Now we need to handle what happens if there's an item already at the destination path
		//In this instance, we do what the doclib does when uploading items and add a -X to the end 
		//of the filename, incrementing x until we get a unique path, subject to a hard-coded limit
		int dedupeAttempts=0;
		Path dedupePath = destination.subPath(destination.size()-1); //This effectiveley copies the path as paths index from 0 so size-1 is the last element in the path

		while (_nodeFinder.getNodeRef(destination, NodeFinder.SearchCase.CASE_INSENSITIVE)!=null && ++dedupeAttempts<MAX_PATH_DEDUPE_LIMIT)
		{
			//Copy everything but the last element (again, paths index from 0 so -2 is the last but one element)
			destination = dedupePath.subPath(dedupePath.size()-2);
			//Create a new element, adding -X to the name
			String nameStr=dedupePath.last().getElementString();
			int afterNameSpaceIdx = nameStr.indexOf("}");
			int firstPeriodAfterNameSpaceIdx = nameStr.indexOf(".",afterNameSpaceIdx);
			String newName=null;
			if (firstPeriodAfterNameSpaceIdx>0)
			{
				newName = nameStr.substring(0,firstPeriodAfterNameSpaceIdx)+"-"+dedupeAttempts+nameStr.substring(firstPeriodAfterNameSpaceIdx);
			}
			else
			{
				newName=nameStr+"-"+dedupeAttempts;
			}
			destination.append(new PathElement(newName));
		}
		
		
		if (_logger.isInfoEnabled())
		{
			_logger.info("Destination Path: "+destination);
		}
		return destination;	
	}
	
	/**
	 * Implementation of this method very similar to that of delete
	 */
	@Override
	public void undelete(NodeRef nodeRef) throws ManagedDeletionException {
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Undeleting "+nodeRef);
		}
		
		//Check that the input noderef exists and has been deleted
		if (nodeRef==null || !_nodeService.exists(nodeRef))
		{
			throw new ManagedDeletionException(nodeRef, "Could not undelete the node as it does not exist");
		}

		QName nodeType = _nodeService.getType(nodeRef);
		
		if(ContentModel.TYPE_FOLDER.equals(nodeType)) {
			if(_logger.isDebugEnabled()) {
				_logger.debug("Folder " + nodeRef + " is being undeleted");
			}
			
			List<ChildAssociationRef> children = _nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS,
					RegexQNamePattern.MATCH_ALL);
			
			for(ChildAssociationRef assoc : children) {
				undelete(assoc.getChildRef());
			}
			
			_nodeService.deleteNode(nodeRef);
		} else {
			if (!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED))
			{
				throw new ManagedDeletionException(nodeRef, "Could not undelete the node as it does not have the deleted aspect");
			}
			
			//Get the name of the original site the item was in
			String originalSiteName = (String)(_nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ORIGINAL_SITE_NAME));
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Original Site name: "+originalSiteName);
			}
			
			//Construct a destination path for the item
			Path destinationPath = getUndeleteDesinationPath(_nodeService.getPath(nodeRef), originalSiteName);
			
			// Move the node to it's new (ie. pre-deletion) path
			NodeRef target = moveNode(nodeRef, destinationPath);
				
			//Remove the deleted aspect from the undeleted item
			_nodeService.removeAspect(target, ManagedDeletionModel.ASPECT_DELETED);
		}
	}
	
	/**
	 * Move the given node to the given path
	 * @param toMove The node to move
	 * @param destination Path to move the node to.  This is the path to the destination folder rather than the item
	 * @return NodeRef pointing to the newly created copy of the input NodeRef
	 */
	protected NodeRef moveNode(NodeRef toMove, Path destination)
	{
		NodeRef targetFolder = createParentFoldersAndGetTargetParent(destination);
		if (targetFolder==null)
		{
			throw new ManagedDeletionException(toMove, "Could not find the destination folder at: "+destination);
		}
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Copying item into folder with NodeRef: "+targetFolder);
		}
		
		String originalName = _nodeFinder.getName(destination);
		if (originalName.indexOf("}")!=-1)
		{
			originalName=originalName.substring(originalName.indexOf("}")+1);
		}
		try 
		{
			return _fileFolderService.move(toMove, targetFolder, originalName).getNodeRef();
		}
		catch (FileNotFoundException e)
		{
			throw new ManagedDeletionException(toMove, "Could not copy node to destination at: "+targetFolder);
		}
	}
	
	/**
	 * Split out into a seperate method so it can be overriden for out of context testing in JUnit - java will probably inline this anyway
	 * so shouldn't be a performance hit
	 * @return
	 */
	protected String getCurrentUserName()
	{
		return AuthenticationUtil.getFullyAuthenticatedUser();
	}
	
	/**
	 * Inner class implementation of Element to simplify the creation of Paths
	 * @author simonw
	 *
	 */
	public static class PathElement extends Element
	{
		private static final long serialVersionUID = 1L;

		private String _elementString;
		
		public PathElement(String elementString)
		{
			_elementString=elementString;
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Creating new path element with element string: "+elementString);
			}
		}

		@Override
		public String getElementString() {
			return _elementString;
		}
	}

	@Override
	public void folderContentsChanged(NodeRef folderRef)
	{
		if (_logger.isDebugEnabled()) {
			_logger.debug("FolderContentsChanged called for node " + folderRef);
		}

		if(!_nodeService.hasAspect(folderRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION)) {
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Folder " + folderRef + " does not have mdm:folderMarkedForDeletion aspect - doing nothing");
			}
			return;
		}
		
		_nodeService.removeAspect(folderRef, ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
		
		ChildAssociationRef parentRef = _nodeService.getPrimaryParent(folderRef);
		
		if(parentRef != null) {
			folderContentsChanged(parentRef.getParentRef());
		}
	}
	
	@Override
	public void setPerishable(NodeRef nodeRef, String reason) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("Setting " + nodeRef + " perishable for the reason " + reason);
		}
		
		QName nodeType = _nodeService.getType(nodeRef);
		
		if(!ContentModel.TYPE_CONTENT.equals(nodeType)
				&& !ForumModel.TYPE_TOPIC.equals(nodeType)) {
			throw new ManagedDeletionException(nodeRef, "Only content items and forum topics can be set perishable");
		}
		
		// If reason is null then we remove the perishable aspect
		if(reason == null) {
			if(!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE)) {
				_logger.info(nodeRef + " is not perishable - doing nothing");
				return;
			}
			
			_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE);
		} else {
			if(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE)) {
				_logger.info(nodeRef + " is already perishable - doing nothing");
				return;
			}
			
			if(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED)) {
				_logger.warn(nodeRef + " is already deleted - doing nothing");
				throw new ManagedDeletionException(nodeRef, "Cannot set a deleted item as perishable");
			}
			
			// If this node has previously failed to delete, remove records of that failure
			if (_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE))
			{
				_nodeService.removeAspect(nodeRef, ManagedDeletionModel.ASPECT_FAILED_TO_DELETE);
			}
			
			Date perishDate = _perishabilityLogic.calculatePerishDue(reason, new Date());
			
			HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
			
			properties.put(ManagedDeletionModel.PROP_PERISH_DUE, perishDate);
			properties.put(ManagedDeletionModel.PROP_PERISH_REASON, reason);
			properties.put(ManagedDeletionModel.PROP_PERISH_REQUESTED_BY, getCurrentUserName());
			properties.put(ManagedDeletionModel.PROP_PERISHED_APPLIED, new Date());
			
			if(_logger.isDebugEnabled()) {
				_logger.debug(nodeRef + " will be perished on " + DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(perishDate));
			}
			
			_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE, properties);
		}
	}
	
	@Override
	public NodeArchivalDetails getArchivalDetails(NodeRef nodeRef) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("Getting archive details for " + nodeRef);
		}
		
		boolean isMarkedForDelete = _nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_MARKED_FOR_DELETION);
		boolean isPerishable = _nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_PERISHABLE);

		Date perishDate = (Date) _nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_PERISH_DUE);
		Date deleteDate = (Date) _nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_ARCHIVE_DUE_DATETIME);
		
		if(isMarkedForDelete && isPerishable) {
			if(perishDate.after(deleteDate)) {
				isPerishable = false;	// We will use the delete date
			} else {
				isMarkedForDelete = false;	// We will use the perish date
			}
		}
		
		if(isMarkedForDelete) {
			return new NodeArchivalDetails(ArchivalStatus.MARKED_FOR_DELETE, deleteDate,
					(String) _nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_DELETED_BY), null);
		}
		
		if(isPerishable) {
			return new NodeArchivalDetails(ArchivalStatus.PERISHABLE, perishDate,
					(String) _nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_PERISH_REQUESTED_BY),
					(String) _nodeService.getProperty(nodeRef, ManagedDeletionModel.PROP_PERISH_REASON));
			
		}
		
		if(_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_DELETED)) {
			return new NodeArchivalDetails(ArchivalStatus.DELETED, null, null, null);
		}
		
		return new NodeArchivalDetails(ArchivalStatus.UNMARKED, null, null, null);
	}
	
	@Override
	public void destroy(final NodeRef nodeRef) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("Destroying " + nodeRef);
		}
		
		try {
			_transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Boolean>() {

				@Override
				public Boolean execute() throws Throwable {
					if(_logger.isDebugEnabled()) {
						_logger.debug("Attempting destroy of " + nodeRef);
					}
					
					_nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
					
					// Remove the version history
					if(_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
						if(_logger.isDebugEnabled()) {
							_logger.debug(nodeRef.toString() + " is versionable - deleting aspect and version history");
						}
						_versionService.deleteVersionHistory(nodeRef);
						_nodeService.removeAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE);
					}
					
					_nodeService.deleteNode(nodeRef);
					
					return Boolean.TRUE;
				}
			}, false, false);
			
		} catch(Exception e) {
			_logger.error("An error was encountered while attempting to destroy " + nodeRef, e);
		}
	}
}
