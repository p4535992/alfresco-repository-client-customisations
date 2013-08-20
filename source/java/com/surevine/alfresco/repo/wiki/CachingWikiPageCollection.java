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
