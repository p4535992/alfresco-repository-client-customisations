package com.surevine.alfresco.repo.delete.test;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;

import com.surevine.alfresco.repo.NodeFinder;

public class NodeFinderMock extends NodeFinder {

	private static  int _count=0;
	public NodeFinderMock() {
		_nodeService = new NodeServiceMock();
	}
	
	public NodeRef getNodeRef(final Path path, final SearchCase searchCase) {
		return new NodeRef("workspace", "SpacesStore", "test"+(_count++));
	}

}
