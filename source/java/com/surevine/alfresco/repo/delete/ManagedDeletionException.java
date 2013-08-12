package com.surevine.alfresco.repo.delete;

import org.alfresco.service.cmr.repository.NodeRef;

public class ManagedDeletionException extends RuntimeException {

	private static final long serialVersionUID = 1600342716221707928L;

		public ManagedDeletionException(NodeRef item, String message)
		{
			super("Item: "+item+"  Message: "+message);
		}
		
		public ManagedDeletionException(NodeRef item, String message, Throwable cause)
		{
			super("Item: "+item+"  Message: "+message, cause);
		}
}
