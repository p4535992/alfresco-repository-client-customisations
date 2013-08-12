package com.surevine.alfresco.repo.wiki;

/**
 * Extends the wiki page collection with behaviours associated with caching, which for now just means
 * the ability to invalidate a cache
 * @author simonw
 *
 */
public interface CachingWikiPageCollection extends WikiPageCollection {

	/**
	 * Invalidate one or more caches managed by this object
	 * @param path An indication of the subset of the repository for which changes have caused
	 * the caller to request an invalidation.  Implementation classes may or may not alter their behaviour
	 * based upon the value of this field
	 */
	public void invalidate(String path);
}
