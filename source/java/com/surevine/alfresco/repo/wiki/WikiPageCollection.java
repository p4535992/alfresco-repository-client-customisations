package com.surevine.alfresco.repo.wiki;

import java.util.Collection;

/**
 * This simple interface describes an object which maintains a collection of wiki page names for a series of paths.
 * The term "path" broadly equates to "site"
 * @author simonw
 *
 */
public interface WikiPageCollection {
	
	/**
	 * Get the wiki page names managed by this cache for the given path within the repository
	 * @param path Subset of the repository to retrieve page names for
	 * @return A collection of Strings representing page names
	 */
	public Collection<String> getWikiPageNames(String path);

}
