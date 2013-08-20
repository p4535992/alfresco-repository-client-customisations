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
package com.surevine.alfresco.repo.delete;

import java.util.Date;
import java.util.List;

/**
 * Interface for classes which can manage and calculate pershability parameters.
 */
public interface PerishabilityLogic {
	/**
	 * Returns a calculation of when the perish of an item is due, based on the current time.
	 * @param reasonCode the perish reason
	 * @param currentDate the current datetime
	 * @return the date/time when the perish should happen
	 * @throws ManagedDeletionException if the reason doesn't exist in the list of reasons
	 */
	Date calculatePerishDue(String reasonCode, Date currentDate);
	
	/**
	 * Returns a list of the reasons an item may be perished for a given site.
	 * @param site the inetrnal site name
	 * @return the list of reasons
	 */
	List<PerishReason> getPerishReasons(String site);
	
	/**
	 * Gets a perish reason from a code.
	 * @param reasonCode
	 * @return the perish reason, or null if not found
	 */
	PerishReason getPerishReason(String reasonCode);
}
