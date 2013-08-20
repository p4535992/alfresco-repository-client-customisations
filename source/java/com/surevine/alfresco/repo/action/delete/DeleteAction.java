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
 * Action to wrap a call to ManagedDeletionService.delete(...) in an action.  The action is performed using the permissions of a configurable super-user
 * @author simonw
 *
 */
public class DeleteAction extends ActionExecuterAbstractBase
{

	private static final Log _logger = LogFactory.getLog(DeleteAction.class);
	
	/**
	 * The id of the spring bean this class is instantiated under, which according to our standards will be the fqName of this class.  This is awkward, but
	 * seems to be reqd (certainly, it's how the core Alfresco code works) in order to wrap this action executor into an Action via ActionService, which we 
	 * need to do to schedule it using quartz
	 */
	public static final String NAME = "com.surevine.alfresco.repo.action.delete.DeleteAction";
	
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
	 * This action runs as a super-user, which can be injected or takes the default value "admin".  This is so that we can create items in sites (ie. deleted items sites)
	 * to which we would not normally have access
	 * @param userName ID of the super-user
	 */
	public void setRunAsUser(String userName)
	{
		_runAsUser=userName;
	}
	private String _runAsUser="admin";
	
	/**
	 * Execute the action.  Wraps the work in an ExecuteActionWork so it can be run as the super-user, but ends up as a single
	 * call to ManagedDeletionService
	 */
	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Delete-archiving "+nodeRef+" from action: "+action.getTitle());
		}
		AuthenticationUtil.runAs( new ExecuteActionWork(nodeRef), _runAsUser);
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// Intentionally left blank - no parameters used
	}
	
	private class ExecuteActionWork implements RunAsWork<Boolean>
	{
		private NodeRef _nodeRef;
		public ExecuteActionWork(NodeRef nodeRef)
		{
			_policyFilter.disableBehaviour(ContentModel.ASPECT_VERSIONABLE);
			_policyFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
			_nodeRef=nodeRef;
		}
		
		@Override
		public Boolean doWork() throws Exception {
			try {
				_mdService.delete(_nodeRef);
			}
			catch (ManagedDeletionLockException e)
			{
				_logger.warn("The nodeRef "+_nodeRef+" Could not be deleted due to a locking issue.  This delete will be retried the next time the scheduled job is executed", e);
			}
			return true;
		}	
	}
}
