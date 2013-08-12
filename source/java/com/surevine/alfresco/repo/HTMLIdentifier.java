package com.surevine.alfresco.repo;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Interface describing a class that wraps a NodeRef and has some way of 
 * determining whether the content at that NodeRef is HTML or not.
 * 
 * Implementors of this interface are not guaranteed to be thread-safe
 *
 */
public interface HTMLIdentifier {

	public void setTargetNodeRef(NodeRef target);
	
	public NodeRef getTargetNodeRef();

	/** 
	 * @return True if the target NodeRef exists, is accessible and is HTML.  False otherwise.
	 * @throws IllegalStateException If called before a valid call to setTargetNodeRef
	 */
	public boolean isTargetHTML() throws IllegalStateException;

}
