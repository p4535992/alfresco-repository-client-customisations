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
package com.surevine.alfresco.model;

import org.alfresco.service.namespace.QName;

/**
 * Static references for the managed deletion model
 * @author simonw
 *
 */
public final class ManagedDeletionModel {

	private ManagedDeletionModel() { }
	
	/**
	 * Namespace for extensions used throughout this model
	 */
	public static final String NAMESPACE_MD="http://www.surevine.com/alfresco/model/managedDeletion/1.0";
	
	/**
	 * QName of the Aspect used to identify items which have been marked for deletion
	 */
	public static final QName ASPECT_MARKED_FOR_DELETION = QName.createQName(NAMESPACE_MD, "markedForDeletion");
	
	/**
	 * QName of the Property used to describe when an item is to to be Archived
	 */
	public static final QName PROP_ARCHIVE_DUE_DATETIME = QName.createQName(NAMESPACE_MD, "archivalDue");
	
	/**
	 * QName of the Property used to describe who marked an item for deletion
	 */
	public static final QName PROP_DELETED_BY = QName.createQName(NAMESPACE_MD, "deletedBy");
	
	/**
	 * QName of the Aspect used to identify items which have been deleted by moving them to a deleted items site
	 */
	public static final QName ASPECT_DELETED = QName.createQName(NAMESPACE_MD, "deleted");

	/**
	 * QName of the Property used to record when an item was archived
	 */
	public static final QName PROP_DELETED_TIMESTAMP = QName.createQName(NAMESPACE_MD, "deletedTimestamp");

	/**
	 * QName of the Aspect used to identify folders which should be deleted when they are empty
	 */
	public static final QName ASPECT_FOLDER_MARKED_FOR_DELETION = QName.createQName(NAMESPACE_MD, "folderMarkedForDeletion");
	
	/**
	 * QName of the Property used to describe who marked an item for deletion
	 */
	public static final QName PROP_FOLDER_DELETED_BY = QName.createQName(NAMESPACE_MD, "folderDeletedBy");
	
	/**
	 * QName of the Property used to describe who marked an item for deletion
	 */
	public static final QName PROP_ORIGINAL_SITE_NAME = QName.createQName(NAMESPACE_MD, "originalSiteName");
	
	/**
	 * QName of the Aspect used to identify items which have failed to be deleted by moving them to a deleted items site
	 */
	public static final QName ASPECT_FAILED_TO_DELETE = QName.createQName(NAMESPACE_MD, "failedToDelete");
	
	/**
	 * QName of the Property used to indicate when an item failed to delete
	 */
	public static final QName PROP_DELETE_FAILURE_DATE = QName.createQName(NAMESPACE_MD, "failureDate");
	
	/**
	 * QName of the Property used to indicate why an item failed to delete
	 */
	public static final QName PROP_DELETE_FAILURE_MESSAGE = QName.createQName(NAMESPACE_MD, "failureMessage");
	
	
	/**
	 * QName of the Aspect used to identify items which have been marked perishable
	 */
	public static final QName ASPECT_PERISHABLE = QName.createQName(NAMESPACE_MD, "perishable");
	
	/**
	 * QName of the Property used to indicate when the item should be perished
	 */
	public static final QName PROP_PERISH_DUE = QName.createQName(NAMESPACE_MD, "perishDue");
	
	/**
	 * QName of the Property used to indicate who requested the item be perished
	 */
	public static final QName PROP_PERISH_REQUESTED_BY = QName.createQName(NAMESPACE_MD, "perishRequestedBy");
	
	/**
	 * QName of the Property used to indicate the reason that this item is perishable
	 */
	public static final QName PROP_PERISH_REASON = QName.createQName(NAMESPACE_MD, "perishReason");
	
	/**
	 * QName of the Property used to indicate when an items was marked as perishable (<em>not</em> when it was perished)
	 */
	public static final QName PROP_PERISHED_APPLIED = QName.createQName(NAMESPACE_MD, "perishabilityApplied");
	
	/**
	 * QName of the Property used to indicate when an item is to be validated for perishable reasons definition
	 */
	public static final QName ASPECT_VALIDATE_PERISHABLE_REASONS = QName.createQName(NAMESPACE_MD, "validatePerishableReasons");
}
