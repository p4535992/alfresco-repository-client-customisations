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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMCaveatConfigService;
import com.surevine.alfresco.esl.impl.EnhancedSecurityModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import java.util.Iterator;
import java.util.Random;

public class AssignRandomValidSecurityGroupsAction extends
		ActionExecuterAbstractBase {

	private static final Log LOG = LogFactory.getLog(AssignRandomValidSecurityGroupsAction.class);
	static {
		if (LOG.isDebugEnabled()) {
			LOG.debug(AssignRandomValidSecurityGroupsAction.class + " initialised");
		}
	}
	private Random _randomiser = new Random();
	private NodeService _nodeService;
	private VersionService _versionService;
	private RMCaveatConfigService _caveatService;
	private PersonService _personService; 
	private float _chanceToApplyGivenSecurityGroup = 0.25f;
	private String[] _constraintNames = { "es:validOpenMarkings", "es:validOrganisations", "es:validClosedMarkings" };
	private static int _userIndexOffset=0; //static to persist between runs
	private BehaviourFilter _policyFilter;
	
	public void setPolicyFilter(BehaviourFilter setFilter)	{
		_policyFilter=setFilter;
	}
	
	
	public void setNodeService(NodeService ns) {
		_nodeService = ns;
	}
	
	public void setPersonService(PersonService personService) {
		_personService=personService;
	}

	public void setCaveatService(RMCaveatConfigService caveatService) {
		_caveatService = caveatService;
	}

	public void setVersionService(VersionService vs) {
		_versionService = vs;
	}

	public void setChanceToApplyGivenSecurityGroup(float chance) {
		_chanceToApplyGivenSecurityGroup = chance;
	}

	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {
		try {
			
			
			//First things first, set a modifier for this item.  We try to round-robin all the users
			//This is intended to be run against an item without any security groups assigned.  If run against
			//an item that already has security groups assigned, it will not consider that the selected user may
			//not be able to access the item, and will throw an AccessDenied exception accordingly
			
			Iterator<NodeRef> peopleNodes = _personService.getAllPeople().iterator();
			int skipCount=0;
			while (peopleNodes.hasNext()) {
				NodeRef personNode = peopleNodes.next();
				if (!(skipCount++ < _userIndexOffset)) {
					String userName = _nodeService.getProperty(personNode,  ContentModel.PROP_USERNAME).toString();
					if (LOG.isInfoEnabled()) {
						LOG.info("Setting modifier of "+nodeRef+" to "+userName);
					}
					AuthenticationUtil.runAs(new ModifyItemWork(nodeRef), userName);
					if (!peopleNodes.hasNext()) {
						_userIndexOffset=0;
					}
					else {
						_userIndexOffset++;
					}
					break;
				}
			}
			;
			

			// First, get the list of everyone to have modified this item - we
			// need to make sure all
			// these users could have seen the security marking we will
			// generate, to ensure
			// consistency (actually, we could be more specific than this if we
			// needed to as what we
			// actually need to ensure is that a user who can see Version X can
			// see all versions <X)
			
			Object o = _nodeService.getProperty(nodeRef,
					ContentModel.PROP_MODIFIER);
			if (o == null) {
				o = _nodeService.getProperty(nodeRef,
						ContentModel.PROP_CREATOR);
			}
			final String owner = o.toString();

			Collection<String> modifiers = new HashSet<String>(1);
			try {
				Iterator<Version> allVersions = _versionService
						.getVersionHistory(nodeRef).getAllVersions().iterator();
				while (allVersions.hasNext()) {
					Version v = allVersions.next();
					if (LOG.isDebugEnabled()) {
						LOG.debug("Adding " + v.getFrozenModifier()
								+ " to the list of modifiers for " + nodeRef);
					}
					modifiers.add(v.getFrozenModifier());
				}
			} catch (NullPointerException e) {
				// This means that the item isn't versionable, in which case use
				// the current modifier
				modifiers.add(owner);
			}
			Iterator<String> modifierUserNames;

			// For each security Group, work out the groups to assign
			for (int i = 0; i < _constraintNames.length; i++) {
				modifierUserNames = modifiers.iterator();

				Set<String> potentialGroups = null;

				while (modifierUserNames.hasNext()) {
					String userName = modifierUserNames.next();
					if (potentialGroups == null) {
						potentialGroups = new HashSet<String>(
								AuthenticationUtil.runAs(
										new GetGivenUserSecurityMarkingsWork(
												_constraintNames[i]), userName));
					} else {
						potentialGroups.retainAll(AuthenticationUtil.runAs(
								new GetGivenUserSecurityMarkingsWork(
										_constraintNames[i]), userName));
					}
				}
				
				Iterator<String> potentialGroupsIt = potentialGroups.iterator();
				ArrayList<String> groupsToAdd = new ArrayList<String>(2);
				while (potentialGroupsIt.hasNext()) {
					String potentialGroup = potentialGroupsIt.next();
					if (LOG.isDebugEnabled()) {
						LOG.debug(potentialGroup + " is a potential group for "	+ nodeRef);
					}
					if (_randomiser.nextFloat() < _chanceToApplyGivenSecurityGroup) {
						if (LOG.isInfoEnabled()) {
							LOG.info("Adding " + potentialGroup + " to "+ nodeRef);
						}
						groupsToAdd.add(potentialGroup);
					}
				}
				if (groupsToAdd.contains("ATOMAL2") && !groupsToAdd.contains("ATOMAL1")) {
					groupsToAdd.add("ATOMAL1");
				}
				QName propertyQName = getQNameFromConstraintName(_constraintNames[i]);
				
				if (groupsToAdd.size()>0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Making modification as "+owner);
					}
					//Parts of the renditioned aspects, which require us to have privs to previews etc, require this to be run as the last modifier of the document
					AuthenticationUtil.runAs(new ModifySecurityMarkingWork(nodeRef, propertyQName, groupsToAdd), owner);
				}

			}
			
			//OK, now we've set the security groups - we are now going to munge the modifier of the item
			/*
			 * 
			 * This bit seems to:
			 * 	A) Break whichever site you run it against
			 * 	B) Not consider whether the selected user could actually access the node they are trying to update
			 * 
			*/
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private QName getQNameFromConstraintName(String constraintName) {
		if (constraintName.equals("es:validOpenMarkings")) {
			return EnhancedSecurityModel.PROP_OPEN_GROUPS;
		}
		if (constraintName.equals("es:validClosedMarkings")) {
			return EnhancedSecurityModel.PROP_CLOSED_GROUPS;
		}
		if (constraintName.equals("es:validOrganisations")) {
			return EnhancedSecurityModel.PROP_ORGANISATIONS;
		}
		throw new RuntimeException("The group name " + constraintName + " was unrecognised");
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {

	}

	private class GetGivenUserSecurityMarkingsWork implements
			RunAsWork<Collection<String>> {
		private String _constraintName;

		public GetGivenUserSecurityMarkingsWork(String constraintName) {
			_constraintName = constraintName;
		}

		@Override
		public Collection<String> doWork() throws Exception {
			return _caveatService.getRMAllowedValues(_constraintName);
		}
	}
	
	private class ModifyItemWork implements RunAsWork<Boolean> {
		private NodeRef _item;

		public ModifyItemWork(NodeRef item) {
			_item = item;
		}

		@Override
		public Boolean doWork() throws Exception {
			try { //Set the modifier by setting name=name
				Serializable name = _nodeService.getProperty(_item,  ContentModel.PROP_NAME);
				_nodeService.setProperty(_item,  ContentModel.PROP_NAME, name);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				LOG.error("Could not set the modifier for "+_item, e);
				return Boolean.FALSE;
			}
		}
	}
	
	private class ModifySecurityMarkingWork implements RunAsWork<Boolean> {
		private NodeRef _nodeRef;
		private QName _property;
		private ArrayList<String> _groupsToAdd;
		
		public ModifySecurityMarkingWork(NodeRef nodeRef, QName property, ArrayList<String> toAdd) {
			_nodeRef=nodeRef;
			_property=property;
			_groupsToAdd=toAdd;
		}
		
		public Boolean doWork() throws Exception {
			try {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Setting "+_property+" to "+_groupsToAdd);
				}
				_policyFilter.disableAllBehaviours();
				_nodeService.setProperty(_nodeRef, _property, _groupsToAdd);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				LOG.error("Could not set the security markings of "+_nodeRef, e);
				return Boolean.FALSE;
			}
		}
	}
}
