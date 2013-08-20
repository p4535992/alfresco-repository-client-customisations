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
package com.surevine.alfresco.repo;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Interface describing a class that wraps a NodeRef and has some way of 
 * determining whether the content at that NodeRef is HTML or not.
 * 
 * Implementors of this interface are not guaranteed to be thread-safe
 *
 */
public interface HTMLIdentifier {

	public void setTargetNodeRef(NodeRef target);
	
	public NodeRef getTargetNodeRef();

	/** 
	 * @return True if the target NodeRef exists, is accessible and is HTML.  False otherwise.
	 * @throws IllegalStateException If called before a valid call to setTargetNodeRef
	 */
	public boolean isTargetHTML() throws IllegalStateException;

}
