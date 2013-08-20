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
package com.surevine.alfresco.repo.wiki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple value class that acts as a cache for wiki page names, and their associated node refs
 * @author simonw
 *
 */
public class PageNameCache {

	private static final StoreRef SPACES_STORE= new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private static final Log _logger = LogFactory.getLog(PageNameCache.class);

	
	private Date _lastRefreshTime = new Date(0l);
	private Collection<NameAndNodeRefPair> _pageCache = new ArrayList<NameAndNodeRefPair>(1);
	
	public Date getLastRefreshTime()
	{
		return _lastRefreshTime;
	}
	
	public Collection<NameAndNodeRefPair> getPages()
	{
		return _pageCache;
	}
	
	public void invalidate()
	{
		_pageCache=null;
	}
	
	public void setLastRefreshTime(Date lastRefreshTime)
	{
		_lastRefreshTime=lastRefreshTime;
	}
	
	public void setPages(Collection<NameAndNodeRefPair> pages)
	{
		_pageCache=pages;
	}
	
	/**
	 * Repopulate this cache with pages found under the specified path
	 */
	public void repopulate(String path, SearchService searchService, NodeService nodeService)
	{
		
		//This is the original lucene query used by vanilla alfresco.  We run the query and get an Iterator
		//of NodeRefs representing the results
		String query = "+PATH:\"" + path + "//*\" " +
		   " +(@\\{http\\://www.alfresco.org/model/content/1.0\\}content.mimetype:application/octet-stream OR"
		   + "  @\\{http\\://www.alfresco.org/model/content/1.0\\}content.mimetype:text/html)"
		   + " -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\""
		   +" -TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\"";
		
		SearchParameters sp = new SearchParameters();
		sp.addStore(SPACES_STORE);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setQuery(query);
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Running search: "+query);
		}
				
		//Actually run the search and get the results as an Iterator of NodeRefs
		ResultSet lRS = null;
		List<NodeRef> nrList = null;
		try {
			lRS = searchService.query(sp);
		     nrList = lRS.getNodeRefs();
		}
		finally {
			if (lRS!=null) {
				lRS.close();
			}
		}
		
		Iterator<NodeRef> results = nrList.iterator();
				
		//This collection will be the new cache
		Collection<NameAndNodeRefPair> newEntries = new ArrayList<NameAndNodeRefPair>(nrList.size());
		
		if (_logger.isInfoEnabled())
		{
			_logger.info("Adding "+nrList.size()+" wiki page names to the cache for "+path);
		}
		
		//Repopulate the cache
		while (results.hasNext())
		{
			NodeRef nr = results.next();
			if (nodeService.exists(nr))
			{
				String name = (String)(nodeService.getProperty(nr, ContentModel.PROP_NAME));
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Adding the page "+name+" to the cache for "+path);
				}
				newEntries.add(new NameAndNodeRefPair(name, nr));
			}
			else
			{
				if (_logger.isInfoEnabled())
				{
					_logger.info("The nodeRef "+nr+" no longer exists.  Skipping");
				}
			}
		}
		
		//Now reset the expiry time and overwrite the old cache with the new
		setLastRefreshTime(new Date());
		setPages(newEntries);

	}
}
