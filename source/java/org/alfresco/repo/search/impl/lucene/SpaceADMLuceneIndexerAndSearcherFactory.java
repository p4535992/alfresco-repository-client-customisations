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

import org.alfresco.repo.search.impl.lucene.ADMLuceneIndexerAndSearcherFactory;
import org.alfresco.repo.search.impl.lucene.ADMLuceneIndexerImpl;
import org.alfresco.repo.search.impl.lucene.LuceneIndexer;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Overrides the {@link ADMLuceneIndexerAndSearcherFactory} to use our own
 * indexer.
 * 
 * @author richard.midwinter@surevine.com
 */
public class SpaceADMLuceneIndexerAndSearcherFactory extends ADMLuceneIndexerAndSearcherFactory {
	
	@Override
    protected LuceneIndexer createIndexer(StoreRef storeRef, String deltaId) {
        storeRef = tenantService.getName(storeRef);
        
        final ADMLuceneIndexerImpl indexer = SpaceADMLuceneIndexerImpl.getUpdateIndexer(storeRef, deltaId, this);
        indexer.setNodeService(nodeService);
        indexer.setTenantService(tenantService);
        indexer.setDictionaryService(dictionaryService);
        indexer.setFullTextSearchIndexer(fullTextSearchIndexer);
        indexer.setContentService(contentService);
        indexer.setTransactionService(transactionService);
        indexer.setMaxAtomicTransformationTime(getMaxTransformationTime());
        return indexer;
    }
}
