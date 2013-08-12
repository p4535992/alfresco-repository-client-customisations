package com.surevine.alfresco.repo.profile;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONException;

import com.surevine.ldap2alfresco.ProfileUpdater;
import com.surevine.ldap2alfresco.LdapConnector;
import com.surevine.ldap2alfresco.LongLivedLdapConnector;
import com.surevine.ldap2alfresco.LdapException;
import com.surevine.alfresco.PropertyWrapper;
import com.surevine.alfresco.PropertyException;
import com.surevine.alfresco.model.Group;
import com.surevine.alfresco.model.SecurityModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

public class LDAPUpdaterScriptNode extends BaseScopableProcessorExtension {
	
	private static ProfileUpdater _updater;
	private static LdapConnector _connector;
	protected static final PropertyWrapper PROPERTIES = new PropertyWrapper(ResourceBundle.getBundle("com.surevine.alfresco.repo.profile.ldap"));
	private static final Log _logger = LogFactory.getLog(LDAPUpdaterScriptNode.class);

	
	/**
	 * Lazy singleton retriever for Profile Updater instance
	 * @return
	 */
	protected static ProfileUpdater getUpdater()
	{
		if (_updater==null)
		{		
			try
			{
				_updater = new ProfileUpdater(PROPERTIES);
			}
			catch (PropertyException e)
			{
				throw new RuntimeException("Could not instantiate ProfileUpdater instance due to bad properties", e);
			}
		}
		return _updater;
	}
	
	protected static LdapConnector getConnector()
	{
		if (_connector==null)
		{
			try
			{
				_connector = new LongLivedLdapConnector(PROPERTIES);
			}
			catch (LdapException e)
			{
				throw new RuntimeException("Could not establish a connection to LDAP", e);
			}
		
		}
		return _connector;
	}
	
	public void updateUser(String username, String jsonFields)
	{
		try 
		{
			JSONObject jsonObj = new JSONObject(jsonFields);
			getUpdater().updateSingleUserToLdap(getConnector(), username, jsonObj);
		}
		catch (JSONException je)
		{
			throw new RuntimeException("The following input String could not be parsed as JSON: "+jsonFields, je);
		}
		catch (LdapException le)
		{
			throw new RuntimeException("LDAP update for "+username+" failed", le);
		}
	}
	
	/**
	 * @return The latest security model, if we haven't retrieved one before;
	 * 		or if it could have been updated in LDAP since. Null otherwise.
	 */
	public SecurityModel getSecurityModel()
	{
		return getSecurityModel(true);
	}
	
	public SecurityModel getSecurityModel (final boolean cache)
	{
		try 
		{
			if (!cache|| getConnector().isSecurityModelModified()) {
				SecurityModel securityModel = new SecurityModel();
				securityModel.getOpenMarkings().addAllGroups(getGroupsForType(LdapConnector.GroupType.OPEN));
				securityModel.getClosedMarkings().addAllGroups(getGroupsForType(LdapConnector.GroupType.CLOSED));
				securityModel.getOrganisations().addAllGroups(getGroupsForType(LdapConnector.GroupType.ORG));
				
				getConnector().setSecurityModelUpdated();
				
				return securityModel;
			}
		}
		catch (LdapException le)
		{
			throw new RuntimeException("Could not generate security model from LDAP", le);
		}
		
		return null;	
	}
	
	private Collection<Group> getGroupsForType(LdapConnector.GroupType type) throws LdapException
	{
		Iterator<String> groupNames = getConnector().getAllGroups(type).iterator();
		Collection<Group> rVal = new ArrayList<Group>(10);
		
		while (groupNames.hasNext())
		{
			String name = groupNames.next();
			Group group = new Group();
			group.setName(name);
			try {
				group.setHumanReadableName(getConnector().getHumanName(name,  type));
			}
			catch (LdapException e)
			{
				_logger.warn("Could not derive the human readable name for "+name, e);
			}
			
			try {
				group.setDescription(getConnector().getDescription(name,  type));
			}
			catch (LdapException e)
			{
				_logger.warn("Could not derive the description for "+name, e);

			}
			group.setDisplayType(getConnector().getCategory(name, type));
			try {
				group.setPermissionAuthoritySpecification(getConnector().getPermissionAuthoritiesAsString(name,  type));
			}
			catch (LdapException e)
			{
				_logger.warn("Could not derive the permission authorities for "+name, e);
			}
			
			try {
				group.setDeprecated(getConnector().isDeprecated(name,  type));
			}
			catch (LdapException e)
			{
				_logger.info("Deprecated value for "+name+" wasn't set, so assuming = false", e);
			}
			
			rVal.add(group);
		}
		return rVal;
	}
	
}
