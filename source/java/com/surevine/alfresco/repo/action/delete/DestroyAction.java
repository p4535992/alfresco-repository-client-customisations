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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.repo.delete.ManagedDeletionLockException;
import com.surevine.alfresco.repo.delete.ManagedDeletionService;

/**
 * Action to wrap a call to ManagedDeletionService.destroy(...) in an action.
 * @author ashleyw
 *
 */
public class DestroyAction extends ActionExecuterAbstractBase
{

	private static final Log _logger = LogFactory.getLog(DestroyAction.class);
	
	/**
	 * The id of the spring bean this class is instantiated under, which according to our standards will be the fqName of this class.  This is awkward, but
	 * seems to be reqd (certainley, it's how the core Alfresco code works) in order to wrap this action executor into an Action via ActionService, which we 
	 * need to do to schedule it using quartz
	 */
	public static final String NAME = "com.surevine.alfresco.repo.action.delete.DestroyAction";
	
	private BehaviourFilter _policyFilter;
	public void setPolicyFilter(BehaviourFilter setFilter)
	{
		_policyFilter=setFilter;
	}	
	
	/**
	 * Injected.  The service will will perform the actual work
	 * @param mdService
	 */
	public void setManagedDeletionService(ManagedDeletionService mdService)
	{
		_mdService=mdService;
	}
	private ManagedDeletionService _mdService;
	
	/**
	 * Execute the action.
	 */
	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Destroying "+nodeRef+" from action: "+action.getTitle());
		}
		_policyFilter.disableBehaviour(ContentModel.ASPECT_VERSIONABLE);
		_policyFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
		
		try {
			_mdService.destroy(nodeRef);
		}
		catch (ManagedDeletionLockException e)
		{
			_logger.warn("The nodeRef "+nodeRef+" Could not be destroyed due to a locking issue.", e);
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters used
	}
}
