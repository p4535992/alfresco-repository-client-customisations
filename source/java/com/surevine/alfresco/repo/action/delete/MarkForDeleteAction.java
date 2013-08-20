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
package com.surevine.alfresco.repo.action.delete;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.repo.delete.ManagedDeletionService;

/**
 * Action executor to mark items for deletion, which delegates to ManagedDeletionService
 * @author simonw
 *
 */
public class MarkForDeleteAction extends ActionExecuterAbstractBase {

	
	private static final Log _logger = LogFactory.getLog(MarkForDeleteAction.class);
	
	private ManagedDeletionService _mdService;
	
	public void setManagedDeletionService(ManagedDeletionService mdService)
	{
		_mdService=mdService;
	}
	
	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Marking "+nodeRef+" for deletion from action: "+action.getTitle());
		}
		_mdService.markForDelete(nodeRef);
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters available
	}

}
