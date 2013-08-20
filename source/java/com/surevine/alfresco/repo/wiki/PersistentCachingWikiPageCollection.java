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
import java.util.HashMap;

import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMCaveatConfigService;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements a caching collection of wiki pages for a series of paths.  In this context, a path refers to a subset of the
 * alfresco repository contained under a specific branch of the main root node, with the branch being identified by a path.
 * In it's intended use, these paths will equate to Share Sites.
 * 
 * A seperate cache is maintained for every path.  This class has no knowledge of relationships between paths, so seperate
 * caches would be maintained for the paths "/foo" and "/foo/bar".  In practice, this is irrelevant as paths=sites and sites
 * don't nest.
 * 
 * A cache is created for a path on request, and can be invalidated either by a method call, or by a spring-configurable
 * timeout.  An examination of page.put.json.js will show that the cache for a site is explicitly invalidates when a 
 * page is created in that site.  This allows the timeout to be quite wide as in theory the cache will only become out of date
 * when a page is deleted, which is rare and in any event involves minimal cost.
 * 
 * @author simonw
 *
 */
public class PersistentCachingWikiPageCollection implements CachingWikiPageCollection {
	
	private static final Log _logger = LogFactory.getLog(PersistentCachingWikiPageCollection.class);

	/**
	 * Injected via Spring but a default value provided anyway
	 */
	private long _cacheExpiryTime=1000*60*60; //One hour
	
	/**
	 * Injected
	 */
	private SearchService _searchService;
	
	/**
	 * Injected
	 */
	private NodeService _nodeService;
	
	/**
	 * Injected, id of user to run cache population algorithm as (should be a super-user)
	 */
	private String _cachePopulationUser="admin";
	
	/**
	 * Injected
	 */
	private RMCaveatConfigService _caveatService;
	
	public void setCachePopulationUser(String user)
	{
		_cachePopulationUser=user;
	}
	
	public void setCaveatService(RMCaveatConfigService caveatService)
	{
		_caveatService=caveatService;
	}
	
	public void setCacheExpiryTime(long expiryTimeInMillis)
	{
		_cacheExpiryTime=expiryTimeInMillis;
	}
	
	public void setSearchService(SearchService searchService) {
		_searchService=searchService;
	}
	
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	/**
	 * HashMap mapping paths (ie. Sites) to individual caches
	 */
	private HashMap<String,PageNameCache> _pageCaches = new HashMap<String,PageNameCache>(2);
	
	/**
	 * Note that while the cache is being repopulated, access to this method will be blocked.  This should at
	 * worse case be as quick as the situation before this cache was implemented, as every call used to have to have
	 * to run the cache population search
	 */
	@Override
	public synchronized Collection<String> getWikiPageNames(String path) {
		
		//Get the cache for this path
		PageNameCache cache = _pageCaches.get(path);
		
		//If we don't have a cache for this path, create an empty one
		if (cache==null)
		{
			if (_logger.isInfoEnabled())
			{
				_logger.info("Creating a Wiki Page Name cache for "+path);
			}
			cache = new PageNameCache();
			_pageCaches.put(path, cache);
		}
		
		//Get the contents of the cache
		Collection<NameAndNodeRefPair> pages = cache.getPages();

		if (_logger.isDebugEnabled())
		{
			if (pages==null)
			{
				_logger.debug("Cache for "+path+" is null");
			}
			else {
				_logger.debug("Cache for "+path+" currently contains "+pages.size()+" entries");
			}
		}
		
		//We need to repopulate the cache if it's empty or expired
		if (pages==null || pages.size()==0 || isCacheExpired(path))
		{
			repopulateCache(path);
		}
		
		//We now need to filter the cache to remove items the current user can't see
		//We only need the NodeRefs stored in the cache to perform the visibility logic
		//so we drop those during the filtering process and end up with a collection of Strings
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Filtering cached names");
		}
		
		Collection<NameAndNodeRefPair> original = cache.getPages();
		Collection<String> filteredNames = new ArrayList<String>(original.size());
		
		Iterator<NameAndNodeRefPair> i = original.iterator();
		while (i.hasNext())
		{
			NameAndNodeRefPair pair = i.next();
			if (_nodeService.exists(pair.getNodeRef()) && _caveatService.hasAccess(pair.getNodeRef()))
			{
				filteredNames.add(pair.getName());
			}
		}
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Finished filtering");
		}
		
		return filteredNames;
	}
	
	/**
	 * Has the cache for the given path expired?
	 * @param path Identifier for the cache.  If we're not yet tracking a cache with this ID, return false and log a warning
	 * @return True if the cache is being tracked and was last refreshed more than _cacheExpiryTime ago
	 */
	protected boolean isCacheExpired(String path)
	{
		PageNameCache cache = _pageCaches.get(path);
		if (cache==null)
		{
			_logger.warn("Cache for "+path+" is null so cannot determine expiry time - assuming unexpired");
			return false;
		}
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Details for cache on "+path+":   Last Refresh: "+cache.getLastRefreshTime()+"  Expires on "+new Date(cache.getLastRefreshTime().getTime()+_cacheExpiryTime)+"  Time is now "+new Date());
		}
		return cache.getLastRefreshTime().getTime()+_cacheExpiryTime < new Date().getTime();
	}
	
	/**
	 * Repopulate the cache.  The repopulation is performed as a super-user injected into this class,
	 * which is filtered upon retrieval by getWikiPageNames(...)
	 * @param path
	 */
	protected void repopulateCache(String path)
	{
		try {
			AuthenticationUtil.runAs(new RepopulateWork(path), _cachePopulationUser);
		}
		catch (Exception e)
		{
			//We won't actually throw any exception here but are forced to catch Exception by the RunAsWork interface,
			//hence the poor yet brief handling
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Explicitly invalidates the cache for the given path, regardless of it's expiry time.  In practice, this is done when new
	 * wiki pages are created to ensure that a new page is never missed from the cache
	 */
	public void invalidate(String path)
	{
		PageNameCache cache = _pageCaches.get(path);
		if (cache==null)
		{
			_logger.warn("Attempted to invalidate cache for "+path+" but no cache exists for that path, so doing nothing");
		}
		else
		{
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Cache for "+path+" invalidated");
			}
			cache.invalidate();
		}
	}
	
	/**
	 * The call to repopulate the cache for a given path, encapsulated as a RunAsWork to enable it 
	 * to be run under the auspices of a super-user
	 * @author simonw
	 *
	 */
	private class RepopulateWork implements RunAsWork<Boolean>
	{
		private String path;
		
		public RepopulateWork(String path)
		{
			this.path = path;
		}

		@Override
		public Boolean doWork() throws Exception {
			
			if (_logger.isInfoEnabled())
			{
				_logger.info("Repopulating wiki page name cache for "+path+" ");
			}
			
			PageNameCache cache = _pageCaches.get(path);
			if (cache==null)
			{
				_logger.info("Attempted to repopulate cache for "+path+" but no cache exists for that path, so creating a new one");
				cache = new PageNameCache();
				_pageCaches.put(path, cache);
			}
			
			cache.repopulate(path, _searchService, _nodeService);
			
			if (_logger.isInfoEnabled())
			{
				_logger.info("Cache repopulation complete");
			}
			return true;
		}	
	}
}
