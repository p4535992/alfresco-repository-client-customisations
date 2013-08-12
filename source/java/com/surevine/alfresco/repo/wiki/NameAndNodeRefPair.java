package com.surevine.alfresco.repo.wiki;

import org.alfresco.service.cmr.repository.NodeRef;

public class NameAndNodeRefPair {
	
	private String name;
	private NodeRef nodeRef;
	
	public NameAndNodeRefPair(String name, NodeRef nodeRef)
	{
		this.name=name;
		this.nodeRef=nodeRef;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public NodeRef getNodeRef() {
		return nodeRef;
	}
	
	public void setNodeRef(NodeRef nodeRef) {
		this.nodeRef = nodeRef;
	}

}
