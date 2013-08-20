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

/**
 * Represents the archive details about a node
 * 
 * @author ashleyw
 */
public class NodeArchivalDetails {
	/**
	 * The archival statuses a node could have
	 */
	public enum ArchivalStatus {
		/**
		 * The node is neither marked for delete nor perishable
		 */
		UNMARKED,

		/**
		 * The node is due to be deleted
		 */
		MARKED_FOR_DELETE,

		/**
		 * The node is due to be perished
		 */
		PERISHABLE,

		/**
		 * The node has been archived
		 */
		DELETED
	}

	private final ArchivalStatus status;

	private final Date archivalDue;

	private final String archivalUser;

	private final String perishableReason;

	public NodeArchivalDetails(final ArchivalStatus status,
			final Date archivalDue, final String archivalUser,
			final String perishableReason) {
		super();
		this.status = status;
		this.archivalDue = archivalDue;
		this.archivalUser = archivalUser;
		this.perishableReason = perishableReason;
	}

	/**
	 * Gets the archival status of this item. This reflects the archive reason
	 * that will happen soonest.
	 * <p>
	 * i.e. if the node is both marked for delete and marked perishable, the
	 * status (and other properties of this object) will reflect the one which
	 * will happen first.
	 */
	public ArchivalStatus getStatus() {
		return status;
	}

	/**
	 * Returns the timestamp after which this item will be archived.
	 * 
	 * @return the archive timestamp
	 */
	public Date archivalDue() {
		return archivalDue;
	}

	/**
	 * Returns the username of the user who requested that this node be
	 * archived. This may be the user that set the item as perishable, or it may
	 * be the user that marked the item for delete, depending on which reason
	 * comes first (see {@link #getStatus()})
	 * 
	 * @return the username
	 */
	public String archivalUser() {
		return archivalUser;
	}

	/**
	 * Returns the reason code that this item was made perishable.
	 * 
	 * @return the reason code
	 */
	public String perishableReason() {
		return perishableReason;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((archivalDue == null) ? 0 : archivalDue.hashCode());
		result = prime * result
				+ ((archivalUser == null) ? 0 : archivalUser.hashCode());
		result = prime
				* result
				+ ((perishableReason == null) ? 0 : perishableReason.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NodeArchivalDetails))
			return false;
		NodeArchivalDetails other = (NodeArchivalDetails) obj;
		if (archivalDue == null) {
			if (other.archivalDue != null)
				return false;
		} else if (!archivalDue.equals(other.archivalDue))
			return false;
		if (archivalUser == null) {
			if (other.archivalUser != null)
				return false;
		} else if (!archivalUser.equals(other.archivalUser))
			return false;
		if (perishableReason == null) {
			if (other.perishableReason != null)
				return false;
		} else if (!perishableReason.equals(other.perishableReason))
			return false;
		if (status != other.status)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NodeArchivalDetails [status=" + status + ", archivalDue="
				+ archivalDue + ", archivalUser=" + archivalUser
				+ ", perishableReason=" + perishableReason + "]";
	}
}
