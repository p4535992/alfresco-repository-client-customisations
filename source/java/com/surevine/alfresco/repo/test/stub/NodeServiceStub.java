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
package com.surevine.alfresco.repo.test.stub;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class NodeServiceStub implements NodeService {

	private String _fileName;
	
	public NodeServiceStub(String fileName)
	{
		_fileName=fileName;
	}
	
	public void addAspect(NodeRef arg0, QName arg1,
			Map<QName, Serializable> arg2) throws InvalidNodeRefException,
			InvalidAspectException {
		// Auto-generated method stub intentionally unimplemented

	}

	public ChildAssociationRef addChild(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> addChild(Collection<NodeRef> arg0,
			NodeRef arg1, QName arg2, QName arg3)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public void addProperties(NodeRef arg0, Map<QName, Serializable> arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public AssociationRef createAssociation(NodeRef arg0, NodeRef arg1,
			QName arg2) throws InvalidNodeRefException,
			AssociationExistsException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ChildAssociationRef createNode(NodeRef arg0, QName arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException, InvalidTypeException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ChildAssociationRef createNode(NodeRef arg0, QName arg1, QName arg2,
			QName arg3, Map<QName, Serializable> arg4)
			throws InvalidNodeRefException, InvalidTypeException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public StoreRef createStore(String arg0, String arg1)
			throws StoreExistsException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public void deleteNode(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void deleteStore(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	public boolean exists(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public boolean exists(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public Set<QName> getAspects(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			Set<QName> arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getChildAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2, boolean arg3)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Collection<ChildAssociationRef> getChildAssocsWithoutParentAssocsOfType(
			NodeRef arg0, QName arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public NodeRef getChildByName(NodeRef arg0, QName arg1, String arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getChildrenByName(NodeRef arg0,
			QName arg1, Collection<String> arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Status getNodeStatus(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getParentAssocs(NodeRef arg0)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<ChildAssociationRef> getParentAssocs(NodeRef arg0,
			QNamePattern arg1, QNamePattern arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Path getPath(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<Path> getPaths(NodeRef arg0, boolean arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ChildAssociationRef getPrimaryParent(NodeRef arg0)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Map<QName, Serializable> getProperties(NodeRef arg0)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Serializable getProperty(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException {
		return _fileName;
	}

	public NodeRef getRootNode(StoreRef arg0) throws InvalidStoreRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<AssociationRef> getSourceAssocs(NodeRef arg0, QNamePattern arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public NodeRef getStoreArchiveNode(StoreRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<StoreRef> getStores() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public List<AssociationRef> getTargetAssocs(NodeRef arg0, QNamePattern arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public QName getType(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public boolean hasAspect(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException, InvalidAspectException {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public ChildAssociationRef moveNode(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public void removeAspect(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException, InvalidAspectException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void removeAssociation(NodeRef arg0, NodeRef arg1, QName arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void removeChild(NodeRef arg0, NodeRef arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public boolean removeChildAssociation(ChildAssociationRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public void removeProperty(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public boolean removeSeconaryChildAssociation(ChildAssociationRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public NodeRef restoreNode(NodeRef arg0, NodeRef arg1, QName arg2,
			QName arg3) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public void setChildAssociationIndex(ChildAssociationRef arg0, int arg1)
			throws InvalidChildAssociationRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setProperties(NodeRef arg0, Map<QName, Serializable> arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setProperty(NodeRef arg0, QName arg1, Serializable arg2)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setType(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public List<NodeRef> findNodes(FindNodeParameters arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public AssociationRef getAssoc(Long arg0) {
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
	public Long getNodeAclId(NodeRef arg0) throws InvalidNodeRefException {
		// Auto-generated method stub intentionally unimplemented
		return null;
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
