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
