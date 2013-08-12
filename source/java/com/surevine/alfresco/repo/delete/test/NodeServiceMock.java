package com.surevine.alfresco.repo.delete.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ChildAssociation;
import org.alfresco.service.cmr.dictionary.InvalidAspectException;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.AssociationExistsException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidChildAssociationRefException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.InvalidStoreRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreExistsException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

public class NodeServiceMock implements NodeService {

	private Map<NodeRef, Collection<QName>> _aspects = new HashMap<NodeRef, Collection<QName>>();
	private Map<NodeRef, Map<QName, Serializable>> _properties = new HashMap<NodeRef, Map<QName, Serializable>>();
	private Map<NodeRef, List<ChildAssociationRef>> _childAssocs = new HashMap<NodeRef, List<ChildAssociationRef>>();
	private Map<NodeRef, QName> _types = new HashMap<NodeRef, QName>();
	
	@Override
	public void addAspect(NodeRef nodeRef, QName aspect,
			Map<QName, Serializable> properties) throws InvalidNodeRefException,
			InvalidAspectException {
		Collection<QName> existingAspects = _aspects.get(nodeRef);
		if (existingAspects==null)
		{
			existingAspects = new ArrayList<QName>();
		}
		existingAspects.add(aspect);
		_aspects.put(nodeRef, existingAspects);
		
		Map<QName, Serializable> props = _properties.get(nodeRef);
		
		if(props == null) {
			props = new HashMap<QName, Serializable>();
			_properties.put(nodeRef, props);
		}
		
		if(properties != null) {
			props.putAll(properties);
		}
	}
	
	@Override
	public boolean hasAspect(NodeRef nodeRef, QName aspect) throws InvalidNodeRefException, InvalidAspectException {
		Collection<QName> aspects = _aspects.get(nodeRef);
		if (aspects==null)
		{
			return false;
		}
		return aspects.contains(aspect);
	}
	
	@Override
	public Map<QName, Serializable> getProperties(NodeRef nodeRef) throws InvalidNodeRefException {
		Map<QName, Serializable> props = _properties.get(nodeRef);
		
		if(props != null) {
			return props;
		}
		
		return Collections.emptyMap();
	}
	
	@Override
	public Serializable getProperty(NodeRef nodeRef, QName property) throws InvalidNodeRefException {
		return getProperties(nodeRef).get(property);
	}
	
	@Override
	public void removeAspect(NodeRef nodeRef, QName aspect) throws InvalidNodeRefException, InvalidAspectException {
		Collection<QName> existingAspects = _aspects.get(nodeRef);
		if(existingAspects != null) {
			existingAspects.remove(aspect);
		}
	}
	
	@Override
	public Path getPath(NodeRef arg0) throws InvalidNodeRefException {
		return ManagedDeletionServiceTest.DEFAULT_TEST_PATH;
	}
	
	@Override
	public ChildAssociationRef addChild(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException {
		List<ChildAssociationRef> assocs = _childAssocs.get(arg0);
		
		if(assocs == null) {
			assocs = new ArrayList<ChildAssociationRef>();
			_childAssocs.put(arg0, assocs);
		}
		
		ChildAssociationRef childAssoc = new ChildAssociationRef(arg2, arg0, arg3, arg1);
		
		assocs.add(childAssoc);
		
		return childAssoc;
	}

	@Override
	public List<ChildAssociationRef> addChild(Collection<NodeRef> arg0,
			NodeRef arg1, QName arg2, QName arg3)
			throws InvalidNodeRefException {
		List<ChildAssociationRef> refs = new ArrayList<ChildAssociationRef>();
		for(NodeRef nodeRef : arg0) {
			refs.add(addChild(nodeRef, arg1, arg2, arg3));
		}
		
		return refs;
	}

	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0)
			throws InvalidNodeRefException {
		List<ChildAssociationRef> ref = _childAssocs.get(arg0);
		
		if(ref != null) {
			return ref;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2)
			throws InvalidNodeRefException {
		List<ChildAssociationRef> assocs = new ArrayList<ChildAssociationRef>();

		for(ChildAssociationRef ref : getChildAssocs(arg0)) {
			if(arg1.isMatch(ref.getTypeQName()) && arg2.isMatch(ref.getQName())) {
				assocs.add(ref);
			}
		}
		
		return assocs;
	}

	@Override
	public void setType(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException {
		_types.put(arg0,  arg1);
	}

	@Override
	public QName getType(NodeRef arg0) throws InvalidNodeRefException {
		return _types.get(arg0);
	}

	@Override
	public ChildAssociationRef getPrimaryParent(NodeRef arg0)
			throws InvalidNodeRefException {
		for(Entry<NodeRef,List<ChildAssociationRef>> entry : _childAssocs.entrySet()) {
			for(ChildAssociationRef assoc : entry.getValue()) {
				if(assoc.getChildRef().equals(arg0)) {
					return assoc;
				}
			}
		}
		return null;
	}

	@Override
	public void deleteNode(NodeRef arg0) throws InvalidNodeRefException {
		for(ChildAssociationRef child : getChildAssocs(arg0)) {
			deleteNode(child.getChildRef());
		}
		
		_aspects.remove(arg0);
		_childAssocs.remove(arg0);
		_properties.remove(arg0);
		_types.remove(arg0);
		
		for(Entry<NodeRef,List<ChildAssociationRef>> entry : _childAssocs.entrySet()) {
			Iterator<ChildAssociationRef> i = entry.getValue().iterator();
			
			while(i.hasNext()) {
				if(i.next().getChildRef().equals(arg0)) {
					i.remove();
				}				
			}
		}
	}

	@Override
	public void removeChild(NodeRef arg0, NodeRef arg1)
			throws InvalidNodeRefException {
		List<ChildAssociationRef> refs = getChildAssocs(arg0);
		
		Iterator<ChildAssociationRef> i = refs.iterator();
		
		while(i.hasNext()) {
			ChildAssociationRef ref = i.next();
			if(ref.getChildRef().equals(arg1)) {
				i.remove();
			}
		}
	}

	//All methods from this point on are just null implementations

	@Override
	public void addProperties(NodeRef arg0, Map<QName, Serializable> arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public AssociationRef createAssociation(NodeRef arg0, NodeRef arg1,
			QName arg2) throws InvalidNodeRefException,
			AssociationExistsException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public ChildAssociationRef createNode(NodeRef arg0, QName arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException, InvalidTypeException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public ChildAssociationRef createNode(NodeRef arg0, QName arg1, QName arg2,
			QName arg3, Map<QName, Serializable> arg4)
			throws InvalidNodeRefException, InvalidTypeException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public StoreRef createStore(String arg0, String arg1)
			throws StoreExistsException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public void deleteStore(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public boolean exists(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	@Override
	public boolean exists(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return true;
	}

	@Override
	public List<NodeRef> findNodes(FindNodeParameters arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public Set<QName> getAspects(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public AssociationRef getAssoc(Long arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			Set<QName> arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2, boolean arg3)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildAssocsByPropertyValue(
			NodeRef arg0, QName arg1, Serializable arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public Collection<ChildAssociationRef> getChildAssocsWithoutParentAssocsOfType(
			NodeRef arg0, QName arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public NodeRef getChildByName(NodeRef arg0, QName arg1, String arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildrenByName(NodeRef arg0,
			QName arg1, Collection<String> arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public Long getNodeAclId(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public Status getNodeStatus(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getParentAssocs(NodeRef arg0)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<ChildAssociationRef> getParentAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<Path> getPaths(NodeRef arg0, boolean arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public NodeRef getRootNode(StoreRef arg0) throws InvalidStoreRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<AssociationRef> getSourceAssocs(NodeRef arg0, QNamePattern arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public NodeRef getStoreArchiveNode(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<StoreRef> getStores() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<AssociationRef> getTargetAssocs(NodeRef arg0, QNamePattern arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public ChildAssociationRef moveNode(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public void removeAssociation(NodeRef arg0, NodeRef arg1, QName arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public boolean removeChildAssociation(ChildAssociationRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	@Override
	public void removeProperty(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public boolean removeSeconaryChildAssociation(ChildAssociationRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	@Override
	public NodeRef restoreNode(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public void setChildAssociationIndex(ChildAssociationRef arg0, int arg1)
			throws InvalidChildAssociationRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public void setProperties(NodeRef arg0, Map<QName, Serializable> arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public void setProperty(NodeRef arg0, QName arg1, Serializable arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public NodeRef getNodeRef(Long nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<NodeRef> getAllRootNodes(StoreRef storeRef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef nodeRef,
			QName typeQName, QName qname, int maxResults, boolean preload)
			throws InvalidNodeRefException {
		// TODO Auto-generated method stub
		return null;
	}

}
