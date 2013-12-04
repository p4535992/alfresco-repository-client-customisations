/*
 * Copyright (C) 2013 Surevine Limited.
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
package org.alfresco.repo.search.impl.lucene;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.IndexerException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.ISO9075;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;

/**
 * Index implementation that skips over nodes without parents.
 * 
 * @author richard.midwinter@surevine.com
 */
public class SpaceADMLuceneIndexerImpl extends ADMLuceneIndexerImpl {
    
    static Log s_logger = LogFactory.getLog(SpaceADMLuceneIndexerImpl.class);

    /**
     * Generate an indexer
     * 
     * @param storeRef
     * @param deltaId
     * @param config
     * @return - the indexer instance
     * @throws LuceneIndexException
     */
    public static ADMLuceneIndexerImpl getUpdateIndexer(StoreRef storeRef, String deltaId, LuceneConfig config) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Creating indexer");
        }
        ADMLuceneIndexerImpl indexer = new SpaceADMLuceneIndexerImpl();
        indexer.setLuceneConfig(config);
        indexer.initialise(storeRef, deltaId);
        return indexer;
    }

    private static class Pair<F, S>
    {
        private F first;

        private S second;

        /**
         * Helper class to hold two related objects
         * 
         * @param first
         * @param second
         */
        public Pair(F first, S second)
        {
            this.first = first;
            this.second = second;
        }

        /**
         * Get the first
         * 
         * @return - first
         */
        public F getFirst()
        {
            return first;
        }

        /**
         * Get the second
         * 
         * @return -second
         */
        public S getSecond()
        {
            return second;
        }
    }

    @Override
    public List<Document> createDocuments(final String stringNodeRef, final FTSStatus ftsStatus,
            final boolean indexAllProperties, final boolean includeDirectoryDocuments, final boolean cascade,
            final Set<Path> pathsProcessedSinceFlush,
            final Map<NodeRef, List<ChildAssociationRef>> childAssociationsSinceFlush, final IndexReader deltaReader,
            final IndexReader mainReader)
    {
        if (tenantService.isEnabled() && ((AuthenticationUtil.getRunAsUser() == null) || (AuthenticationUtil.isRunAsUserTheSystemUser())))
        {
            // ETHREEOH-2014 - dictionary access should be in context of tenant (eg. full reindex with MT dynamic
            // models)
            return AuthenticationUtil.runAs(new RunAsWork<List<Document>>()
            {
                public List<Document> doWork()
                {
                    return createDocumentsImpl(stringNodeRef, ftsStatus, indexAllProperties, includeDirectoryDocuments,
                            cascade, pathsProcessedSinceFlush, childAssociationsSinceFlush, deltaReader, mainReader);
                }
            }, tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantService.getDomain(new NodeRef(stringNodeRef).getStoreRef().getIdentifier())));
        }
        else
        {
            return createDocumentsImpl(stringNodeRef, ftsStatus, indexAllProperties, includeDirectoryDocuments,
                    cascade, pathsProcessedSinceFlush, childAssociationsSinceFlush, deltaReader, mainReader);
        }
    }

    @SuppressWarnings("deprecation")
	private List<Document> createDocumentsImpl(final String stringNodeRef, FTSStatus ftsStatus,
            boolean indexAllProperties, boolean includeDirectoryDocuments, final boolean cascade,
            final Set<Path> pathsProcessedSinceFlush,
            final Map<NodeRef, List<ChildAssociationRef>> childAssociationsSinceFlush, final IndexReader deltaReader,
            final IndexReader mainReader)
    {
        final NodeRef nodeRef = new NodeRef(stringNodeRef);
        final NodeRef.Status nodeStatus = nodeService.getNodeStatus(nodeRef);             // DH: Let me know if this field gets dropped (performance)
        final List<Document> docs = new LinkedList<Document>();
        if (nodeStatus == null)
        {
            throw new InvalidNodeRefException("Node does not exist: " + nodeRef, nodeRef);            
        }
        else if (nodeStatus.isDeleted())
        {
            // If we are being called in non FTS mode on a deleted node, we must still create a new FTS marker
            // document, in case FTS is currently in progress and about to restore our node!
            addFtsStatusDoc(docs, ftsStatus, nodeRef, nodeStatus);
            return docs;
        }

        final Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

        boolean isRoot = nodeRef.equals(tenantService.getName(nodeService.getRootNode(nodeRef.getStoreRef())));

        // Generate / regenerate all applicable parent paths as the system user (the current user doesn't necessarily have access
        // to all of these)
        if (includeDirectoryDocuments)
        {
            AuthenticationUtil.runAs(new RunAsWork<Void>()
            {
                @Override
                public Void doWork() throws Exception
                {
                    // We we must cope with the possibility of the container not existing for some of this node's parents
                    for (ChildAssociationRef assocRef: nodeService.getParentAssocs(nodeRef))
                    {
                        NodeRef parentRef = tenantService.getName(assocRef.getParentRef());
                        if (!childAssociationsSinceFlush.containsKey(parentRef))
                        {
                            String parentRefSString = parentRef.toString();
                            if (!locateContainer(parentRefSString, deltaReader)
                                    && !locateContainer(parentRefSString, mainReader))
                            {
                                generateContainersAndBelow(nodeService.getPaths(parentRef, false), docs, false,
                                        pathsProcessedSinceFlush, childAssociationsSinceFlush);
                            }
                        }
                    }
                    
                    // Now regenerate the containers for this node, cascading if necessary
                    // Only process 'containers' - not leaves
                    if (isCategory(getDictionaryService().getType(nodeService.getType(nodeRef)))
                            || mayHaveChildren(nodeRef)
                            && !getCachedChildren(childAssociationsSinceFlush, nodeRef).isEmpty())
                    {
                        generateContainersAndBelow(nodeService.getPaths(nodeRef, false), docs, cascade,
                                pathsProcessedSinceFlush, childAssociationsSinceFlush);
                    }
                    
                    return null;
                }
            }, tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantService.getDomain(nodeRef
                    .getStoreRef().getIdentifier())));
        }

        Document xdoc = new Document();
        xdoc.add(new Field("ID", stringNodeRef, Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        xdoc.add(new Field("TX", nodeStatus.getChangeTxnId(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        boolean isAtomic = true;
        for (QName propertyName : properties.keySet())
        {
            Serializable value = properties.get(propertyName);

            value = convertForMT(propertyName, value);

            if (indexAllProperties)
            {
                indexProperty(nodeRef, propertyName, value, xdoc, false);
            }
            else
            {
                isAtomic &= indexProperty(nodeRef, propertyName, value, xdoc, true);
            }
        }

        StringBuilder qNameBuffer = new StringBuilder(64);
        StringBuilder assocTypeQNameBuffer = new StringBuilder(64);

        if (!isRoot)
        {
            for (Pair<ChildAssociationRef, QName> pair : getAllParents(nodeRef, properties))
            {
                ChildAssociationRef qNameRef = tenantService.getName(pair.getFirst());
                if ((qNameRef != null) && (qNameRef.getParentRef() != null) && (qNameRef.getQName() != null))
                {
                    if (qNameBuffer.length() > 0)
                    {
                        qNameBuffer.append(";/");
                        assocTypeQNameBuffer.append(";/");
                    }
                    qNameBuffer.append(ISO9075.getXPathName(qNameRef.getQName()));
                    assocTypeQNameBuffer.append(ISO9075.getXPathName(qNameRef.getTypeQName()));
                    xdoc.add(new Field("PARENT", qNameRef.getParentRef().toString(), Field.Store.YES,
                            Field.Index.NO_NORMS, Field.TermVector.NO));
                    // xdoc.add(new Field("ASSOCTYPEQNAME", ISO9075.getXPathName(qNameRef.getTypeQName()),
                    // Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                    xdoc.add(new Field("LINKASPECT", (pair.getSecond() == null) ? "" : ISO9075.getXPathName(pair
                            .getSecond()), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                }
            }

        }

        // Root Node
        if (isRoot)
        {
            // TODO: Does the root element have a QName?
            xdoc.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
            xdoc.add(new Field("PATH", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("QNAME", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("ISROOT", "T", Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
            xdoc.add(new Field("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(ContentModel.ASSOC_CHILDREN), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
            docs.add(xdoc);

        }
        else
        // not a root node
        {
            final ChildAssociationRef primary = nodeService.getPrimaryParent(nodeRef);

            if (primary == null || primary.getParentRef() == null || tenantService.getName(primary.getParentRef()) == null)
            {
                s_logger.warn(String.format("Skipping node %s without parent.", stringNodeRef));
            }
            else
            {
                s_logger.debug(String.format("Processing node %s.", stringNodeRef));
                xdoc.add(new Field("QNAME", qNameBuffer.toString(), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("ASSOCTYPEQNAME", assocTypeQNameBuffer.toString(), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                // xdoc.add(new Field("PARENT", parentBuffer.toString(), true, true,
                // true));

                xdoc.add(new Field("PRIMARYPARENT", tenantService.getName(primary.getParentRef()).toString(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                xdoc.add(new Field("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(primary.getTypeQName()), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                QName typeQName = nodeService.getType(nodeRef);

                xdoc.add(new Field("TYPE", ISO9075.getXPathName(typeQName), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                for (QName classRef : nodeService.getAspects(nodeRef))
                {
                    xdoc.add(new Field("ASPECT", ISO9075.getXPathName(classRef), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                }

                xdoc.add(new Field("ISROOT", "F", Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
                xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));

                // Record the need for FTS on this node and transaction with a supplementary document. That way we won't
                // forget about it if FTS is already in progress for an earlier transaction!
                if (!isAtomic && !indexAllProperties)
                {
                    addFtsStatusDoc(docs, ftsStatus, nodeRef, nodeStatus);
                }

                // {
                docs.add(xdoc);
                // }
            }
        }
        return docs;
    }
    
    @SuppressWarnings("deprecation")
	private void generateContainersAndBelow(List<Path> paths, List<Document> docs, boolean cascade,
            Set<Path> pathsProcessedSinceFlush, Map<NodeRef, List<ChildAssociationRef>> childAssociationsSinceFlush)
    {
        if (paths.isEmpty())
        {
            return;
        }

        for (Path path: paths)
        {
            NodeRef nodeRef = tenantService.getName(((ChildAssocElement) path.last()).getRef().getChildRef());
            
            // Prevent duplication of path cascading
            if (pathsProcessedSinceFlush.add(path))
            {
                // Categories have special powers - generate their container regardless of their actual children
                boolean isCategory = isCategory(getDictionaryService().getType(nodeService.getType(nodeRef)));

                // For other containers, we only add a doc if they actually have children
                if (!isCategory)
                {
                    // Only process 'containers' - not leaves
                    if (!mayHaveChildren(nodeRef))
                    {
                        continue;
                    }
        
                    // Only process 'containers' - not leaves
                    if (getCachedChildren(childAssociationsSinceFlush, nodeRef).isEmpty())
                    {
                        continue;
                    }
                }

                // Skip the root, which is a single document
                if (path.size() > 1)
                {
                    String pathString = path.toString();    
                    if ((pathString.length() > 0) && (pathString.charAt(0) == '/'))
                    {
                        pathString = pathString.substring(1);
                    }
                    Document directoryEntry = new Document();
                    directoryEntry.add(new Field("ID", nodeRef.toString(), Field.Store.YES,
                            Field.Index.NO_NORMS, Field.TermVector.NO));
                    directoryEntry.add(new Field("PATH", pathString, Field.Store.YES, Field.Index.TOKENIZED,
                            Field.TermVector.NO));
                    for (NodeRef parent : getParents(path))
                    {
                        directoryEntry.add(new Field("ANCESTOR", tenantService.getName(parent).toString(),
                                Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
                    }
                    directoryEntry.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.NO_NORMS,
                            Field.TermVector.NO));
            
                    if (isCategory)
                    {
                        directoryEntry.add(new Field("ISCATEGORY", "T", Field.Store.YES, Field.Index.NO_NORMS,
                                Field.TermVector.NO));
                    }
            
                    docs.add(directoryEntry);
                }
            }
        
            if (cascade)
            {
                List<Path> childPaths = new LinkedList<Path>();
                for (ChildAssociationRef childRef : getCachedChildren(childAssociationsSinceFlush, nodeRef))
                {
                    childPaths.add(new Path().append(path).append(new Path.ChildAssocElement(childRef)));
                }
                generateContainersAndBelow(childPaths, docs, true, pathsProcessedSinceFlush,
                        childAssociationsSinceFlush);
            }
        }
    }

    private List<ChildAssociationRef> getCachedChildren(
            Map<NodeRef, List<ChildAssociationRef>> childAssociationsSinceFlush, NodeRef nodeRef)
    {
        List <ChildAssociationRef> children = childAssociationsSinceFlush.get(nodeRef);

        // Cache the children in case there are many paths to the same node
        if (children == null)
        {
            children = nodeService.getChildAssocs(nodeRef);
            for (ChildAssociationRef childRef : children)
            {
                // We don't want index numbers in generated paths
                childRef.setNthSibling(-1);                
            }
            childAssociationsSinceFlush.put(nodeRef, children);
        }
        return children;
    }

    @SuppressWarnings("deprecation")
	private void addFtsStatusDoc(List<Document> docs, FTSStatus ftsStatus, NodeRef nodeRef,
            NodeRef.Status nodeStatus)
    {
        // If we are being called during FTS failover, then don't bother generating a new doc
        if (ftsStatus == FTSStatus.Clean)
        {
            return;
        }
        Document doc = new Document();
        doc.add(new Field("ID", GUID.generate(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        doc.add(new Field("FTSREF", nodeRef.toString(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        doc
                .add(new Field("TX", nodeStatus.getChangeTxnId(), Field.Store.YES, Field.Index.NO_NORMS,
                        Field.TermVector.NO));
        doc.add(new Field("FTSSTATUS", ftsStatus.name(), Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
        docs.add(doc);
    }
    
    private Serializable convertForMT(QName propertyName, Serializable inboundValue)
    {
        if (!tenantService.isEnabled())
        {
            // no conversion
            return inboundValue;
        }

        PropertyDefinition propertyDef = getDictionaryService().getProperty(propertyName);
        if ((propertyDef != null)
                && ((propertyDef.getDataType().getName().equals(DataTypeDefinition.NODE_REF)) || (propertyDef.getDataType().getName().equals(DataTypeDefinition.CATEGORY))))
        {
            if (inboundValue instanceof Collection)
            {
                @SuppressWarnings("unchecked")
				Collection<NodeRef> in = (Collection<NodeRef>) inboundValue;
                ArrayList<NodeRef> out = new ArrayList<NodeRef>(in.size());
                for (NodeRef o : in)
                {
                    out.add(tenantService.getName(o));
                }
                return out;
            }
            else
            {
                return tenantService.getName((NodeRef) inboundValue);
            }
        }

        return inboundValue;
    }

    /**
     * Does the node type or any applied aspect allow this node to have child associations?
     * 
     * @param nodeRef
     * @return true if the node may have children
     */
    private boolean mayHaveChildren(NodeRef nodeRef)
    {
        // 1) Does the type support children?
        QName nodeTypeRef = nodeService.getType(nodeRef);
        TypeDefinition nodeTypeDef = getDictionaryService().getType(nodeTypeRef);
        if ((nodeTypeDef != null) && (nodeTypeDef.getChildAssociations().size() > 0))
        {
            return true;
        }
        // 2) Do any of the applied aspects support children?
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        for (QName aspect : aspects)
        {
            AspectDefinition aspectDef = getDictionaryService().getAspect(aspect);
            if ((aspectDef != null) && (aspectDef.getChildAssociations().size() > 0))
            {
                return true;
            }
        }
        return false;
    }

    private ArrayList<NodeRef> getParents(Path path)
    {
        ArrayList<NodeRef> parentsInDepthOrderStartingWithSelf = new ArrayList<NodeRef>(8);
        for (Iterator<Path.Element> elit = path.iterator(); elit.hasNext(); /**/)
        {
            Path.Element element = elit.next();
            if (!(element instanceof Path.ChildAssocElement))
            {
                throw new IndexerException("Confused path: " + path);
            }
            Path.ChildAssocElement cae = (Path.ChildAssocElement) element;
            parentsInDepthOrderStartingWithSelf.add(0, tenantService.getName(cae.getRef().getChildRef()));

        }
        return parentsInDepthOrderStartingWithSelf;
    }

    private Collection<Pair<ChildAssociationRef, QName>> getAllParents(NodeRef nodeRef, Map<QName, Serializable> properties)
    {
        List<Pair<ChildAssociationRef, QName>> allParents = new LinkedList<Pair<ChildAssociationRef, QName>>();
        // First get the real parents
        StoreRef storeRef = nodeRef.getStoreRef();
        Set<NodeRef> allRootNodes = nodeService.getAllRootNodes(storeRef);
        for (ChildAssociationRef assocRef : nodeService.getParentAssocs(nodeRef))
        {
            allParents.add(new Pair<ChildAssociationRef, QName>(assocRef, null));            

            // Add a fake association to the store root if a real parent is a 'fake' root
            NodeRef parentRef = tenantService.getBaseName(assocRef.getParentRef());
            if (allRootNodes.contains(parentRef))
            {
                NodeRef rootNodeRef = nodeService.getRootNode(parentRef.getStoreRef());
                if (!parentRef.equals(rootNodeRef))
                {
                    allParents.add(new Pair<ChildAssociationRef, QName>(new ChildAssociationRef(
                            assocRef.getTypeQName(), rootNodeRef, assocRef.getQName(), nodeRef), null));
                }
            }
        }

        // Now add the 'fake' parents, including their aspect QName
        for (QName classRef : nodeService.getAspects(nodeRef))
        {
            AspectDefinition aspDef = getDictionaryService().getAspect(classRef);
            if (isCategorised(aspDef))
            {
                for (PropertyDefinition propDef : aspDef.getProperties().values())
                {
                    if (propDef.getDataType().getName().equals(DataTypeDefinition.CATEGORY))
                    {
                        for (NodeRef catRef : DefaultTypeConverter.INSTANCE.getCollection(NodeRef.class, properties.get(propDef.getName())))
                        {
                            if (catRef != null)
                            {
                                // can be running in context of System user, hence use input nodeRef
                                catRef = tenantService.getName(nodeRef, catRef);

                                try
                                {
                                    for (ChildAssociationRef assocRef : nodeService.getParentAssocs(catRef))
                                    {
                                        allParents
                                                .add(new Pair<ChildAssociationRef, QName>(new ChildAssociationRef(assocRef.getTypeQName(), assocRef.getChildRef(), QName.createQName("member"), nodeRef), aspDef.getName()));
                                    }
                                }
                                catch (InvalidNodeRefException e)
                                {
                                    // If the category does not exists we move on the next
                                }

                            }
                        }
                    }
                }
            }
        }
        return allParents;
    }

    private boolean isCategorised(AspectDefinition aspDef)
    {
        if(aspDef == null)
        {
            return false;
        }
        AspectDefinition current = aspDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.ASPECT_CLASSIFIABLE))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getAspect(parentName);
            }
        }
        return false;
    }

    private boolean isCategory(TypeDefinition typeDef)
    {
        if (typeDef == null)
        {
            return false;
        }
        TypeDefinition current = typeDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.TYPE_CATEGORY))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getType(parentName);
            }
        }
        return false;
    }
}
