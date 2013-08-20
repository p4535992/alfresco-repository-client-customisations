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
package com.surevine.alfresco.repo.action;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * Action executor to mark items for perishable reason validation.
 * 
 * @author richardm
 */
public class MarkForPerishableValidationAction extends ActionExecuterAbstractBase {
	
	private static final Log _logger = LogFactory.getLog(MarkForPerishableValidationAction.class);
	
	private NodeService _nodeService;
	
	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}
	
	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {
		if (_logger.isDebugEnabled()) {
			_logger.debug("Marking "+nodeRef+" for perishable reason validation");
		}
		
		if(!_nodeService.hasAspect(nodeRef, ManagedDeletionModel.ASPECT_VALIDATE_PERISHABLE_REASONS)) {
			_nodeService.addAspect(nodeRef, ManagedDeletionModel.ASPECT_VALIDATE_PERISHABLE_REASONS, null);
		} else if (_logger.isDebugEnabled()) {
			_logger.debug("Folder " + nodeRef + " already has the perishableReasons aspect");
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters available
	}
}
