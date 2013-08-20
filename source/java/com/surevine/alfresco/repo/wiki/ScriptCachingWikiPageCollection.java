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
