package com.surevine.alfresco.repo.wiki;

import java.util.Collection;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;;

/**
 * Simple class to expose a wrapped CachingWikiPageCollection to the javascript API.
 * Delegates all of it's work to the injected implementation class
 * @author simonw
 *
 */
public class ScriptCachingWikiPageCollection extends BaseScopableProcessorExtension
		implements CachingWikiPageCollection {

	/**
	 * Injected
	 */
	private CachingWikiPageCollection _implementation;
	
	public void setImplementation(CachingWikiPageCollection implementation)
	{
		_implementation=implementation;
	}
	
	@Override
	public Collection<String> getWikiPageNames(String path) {
		return _implementation.getWikiPageNames(path);
	}
	
	@Override
	public void invalidate(String path)
	{
		_implementation.invalidate(path);
	}

}
