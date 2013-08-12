package com.surevine.alfresco.repo.delete;

import org.alfresco.service.cmr.repository.NodeRef;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * A ManagedDeletionService performs the business logic required to move items around the various states of deletion and undeletion, as specified by 
 * <pre>https://space.surevine.net/share/proxy/alfresco/api/node/content/workspace/SpacesStore/622773bd-9ab1-44e8-aabb-a23fb23b93f7/DeletingUserStories.docx</pre>
 * 
 * It is worth noting that the nature of the link between a regular site and a deleted items site, or even the notion of having seperate sites, is not defined by this
 * interface and is in fact an implementation responsibility
 * 
 * @author simonw
 *
 */
public interface ManagedDeletionService {
	
	/**
	 * Place a record on the given NodeRef indicating that the caller believes the item behind the NodeRef should be deleted at some point in the future.  
	 * Implementations may tighten up the "some point in the future" requirement as part of their implementation.
	 * <p>If the {@link NodeRef} is a cm:Folder then all the items within the folder will be marked for delete
	 * @param nodeRef NodeRef to mark for deletion
	 */
	void markForDelete(NodeRef nodeRef);
	
	/**
	 * Remove any marks that have been placed on the given NodeRef by previous calls to {@link #markForDelete(NodeRef)}.  This effectively indicates that the caller has
	 * decided that the item behind the given NodeRef should not be deleted.
	 * <p>If the {@link NodeRef} is not currently marked for delete then nothing happens.
	 * <p>If the {@link NodeRef} is a folder which has previously been marked for delete then all the items
	 * within the folder marked for delete will have the deletion mark removed.
	 * @param nodeRef NodeRef of the item to un-mark for deletion
	 */
	void removeDeletionMark(NodeRef nodeRef);
	
	/**
	 * Delete a given item that has a markForDelete mark on it.  The exact semantics of the delete operation are left to implementors, but the delete operation
	 * must be reversible
	 * @param nodeRef NodeRef of an item to delete
	 * @throws ManagedDeletionException If the given nodeRef is not marked for deletion
	 */
	NodeRef delete(NodeRef nodeRef) throws ManagedDeletionException;
	
	/**
	 * Reverse the deletion of the specified item
	 * @param nodeRef
	 * @throws ManagedDeletionException If the given nodeRef has not been deleted
	 */
	void undelete(NodeRef nodeRef) throws ManagedDeletionException;
	
	/**
	 * Called if the contents of a folder with the {@link ManagedDeletionModel#ASPECT_FOLDER_MARKED_FOR_DELETION} aspect
	 * are changed. Removes the deletion mark from the folder and all parent folders.
	 * @param folderRef
	 */
	void folderContentsChanged(NodeRef folderRef);
	
	/**
	 * Sets the given item as perishable
	 * @param noderef the {@link NodeRef} to set as perishable
	 * @param reason the textual code for the reason this item is perishable, or <code>null</code> to remove the perishable mark.
	 */
	void setPerishable(NodeRef noderef, String reason);
	
	/**
	 * Gets the details about the archiving status of the node
	 * @param nodeRef
	 * @return the details
	 */
	NodeArchivalDetails getArchivalDetails(NodeRef nodeRef);
	
	/**
	 * Will destroy a node, making it unavailable to the system.
	 * @param nodeRef
	 */
	void destroy(NodeRef nodeRef);
}
