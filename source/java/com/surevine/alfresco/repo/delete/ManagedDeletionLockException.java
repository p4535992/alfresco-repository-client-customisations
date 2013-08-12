package com.surevine.alfresco.repo.delete;

import org.alfresco.service.cmr.repository.NodeRef;

public class ManagedDeletionLockException extends ManagedDeletionException {

	public ManagedDeletionLockException(NodeRef item, String message,
			Throwable cause) {
		super(item, message, cause);
	}

}
