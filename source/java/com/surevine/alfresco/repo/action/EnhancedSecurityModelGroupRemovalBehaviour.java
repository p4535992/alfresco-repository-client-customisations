package com.surevine.alfresco.repo.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import com.surevine.alfresco.esl.exception.EnhancedSecurityException;
import com.surevine.alfresco.esl.impl.GroupDetails;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnhancedSecurityModelGroupRemovalBehaviour implements ContentServicePolicies.OnContentUpdatePolicy {

	private static final Log LOGGER = LogFactory.getLog(EnhancedSecurityModelGroupRemovalBehaviour.class);

	public static final String GROUP_NAME_REGEX="systemName=[_A-Z0-9]+";
	
	private String _namespace="http://www.surevine.com/alfresco/model/previousSecurityGroups/1.0";	
	private PolicyComponent _policyComponent;
	private NodeService _nodeService;
	private QName _aspectQName = QName.createQName(_namespace, "groups");
	private QName _authorisedGroupsToRemoveQName=QName.createQName(_namespace, "deleteAuthorised");
	private ContentService _contentService;
	private QName _previousValuesPropertyName = QName.createQName(_namespace, "previousValues"); //Remove the 'es:'


	/** Defaults to "http://www.surevine.com/alfresco/model/previousSecurityGroups/1.0", inject to override.  Only needs to be set if bindOnInitialisation==true
	 *  
	 * @param namespace
	 */
	public void setNamespace(String namespace)
	{
		_namespace=namespace;
		_aspectQName = QName.createQName(_namespace, "groups");
		_authorisedGroupsToRemoveQName=QName.createQName(_namespace, "lastRemovedGroups");
	}
	
	public QName getAspectQName()
	{
		return _aspectQName;
	}
	
	public String getNamespace()
	{
		return _namespace;
	}
	
	/**
	 * Injected
	 * @param policyComponent
	 */
	public void setPolicyComponent(PolicyComponent policyComponent)
	{
		_policyComponent = policyComponent;
	}
	
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	
	/**
	 * Injected
	 * @param nodeService
	 */
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	/**
	 * Register this behaviour with the policy component, if we're so configured - otherwise, just log and do nothing.
	 */
	public void init()
	{
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Initialising Enhanced Security Customer Specific Validation bean");
		}
		
		/**
		 * Fire the policy when content is updated, at the end of the transaction, for items with the specified aspect
		 */
        _policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                _aspectQName,
                new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        
        if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Enhanced Security Customer Specific Validation bean initialised");
		}
	}

	/**
	 * Only allow the update if no previous groups, except those listed in the authorisedDeletion property, have been removed.  We only care about removal
	 * from the model - it's OK to change the type of a group.
	 * 
	 * Additionally, please note that groups that are commented out are _NOT_ counted as having been removed.  This shouldn't be a problem ibn production,
	 * but could cause confusion during test
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		if (_nodeService.exists(nodeRef)) { // Q.Only interested in? workspace://SpacesStore/enhanced_security_custom_model
											// A. Sorta.  We're interested in anything with the psg:groups aspect applied, which will (at time of writing) only be one file.
											//    This is taken care of us by the call to policyComponent in the init() method so we don't have to worry about it here, if we bind on init.
											//    If we don't bind on init, then it's up to the caller to call us at the right time!
			
			//Get the new content and load it into a string
			ContentReader reader = _contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			String newContentString = reader.getContentString();
			LOGGER.trace("New Model: "+newContentString);
			Collection<String> valuesAfterUpdate = getGroupNamesFromModelContent(newContentString);
			
			//Get (and log) the list of groups it's OK to remove
			@SuppressWarnings("rawtypes")
			Collection<String> authorisedGroupsToRemove=(Collection)(_nodeService.getProperty(nodeRef, _authorisedGroupsToRemoveQName));
			if (LOGGER.isDebugEnabled())
			{
				String s ="undefined";
				if (authorisedGroupsToRemove!=null)
				{
					s=StringUtils.join(authorisedGroupsToRemove.toArray(), ",");
					LOGGER.debug(authorisedGroupsToRemove.size()+" authorised groups to remove:  "+s);
				}
				else 
				{
					LOGGER.debug("No groups are authorised for removal");
				}
			}

			// This is the set of values allowed after the update. Usually retrieved from LDAP.
			final String allowedValues = StringUtils.join(valuesAfterUpdate.toArray(), ',');
			LOGGER.debug("The Model is now allowed the values: " + allowedValues);

			//Get the previous values.  if it's not present, just allow the update
			Object previousValuesPropVal = _nodeService.getProperty(nodeRef, _previousValuesPropertyName);						
			if (previousValuesPropVal == null) 
			{
				LOGGER.debug("Existing property: " + _previousValuesPropertyName + " could not be found");
			} else {
				Collection<String> valuesBeforeUpdate = Arrays.asList(previousValuesPropVal.toString().split(","));

				// This is the set of values previously allowed. Retrieved from Alfresco.
				LOGGER.debug("The model was allowed the values: " + previousValuesPropVal.toString());
				
				//For each group before the update, if it's not in the file after the update, then see if it's in the list of groups to delete.  If it isn't, abort
				Iterator<String> groupNamesBeforeUpdate = valuesBeforeUpdate.iterator();
				while (groupNamesBeforeUpdate.hasNext())
				{
					String groupName = groupNamesBeforeUpdate.next();
					LOGGER.debug("Checking for the presence of: "+groupName);
					if (!safeContains(groupName, valuesAfterUpdate))
					{
						if (authorisedGroupsToRemove!=null && safeContains(groupName, authorisedGroupsToRemove))
						{
							authorisedGroupsToRemove.remove(groupName);
							_nodeService.setProperty(nodeRef,  _authorisedGroupsToRemoveQName, new ArrayList<String>(authorisedGroupsToRemove));
						}
						else 
						{	
							throw new EnhancedSecurityException("The group: "+groupName+" was removed and was not in the authorised removal list");
						}
					}
				}
			}
			
			_nodeService.setProperty(nodeRef, _previousValuesPropertyName, allowedValues.toString());
			
		} else {
			LOGGER.debug("The node does not exist: " + nodeRef);
		}
	}
	
	/**
	 * Basically a fluffed .contains() that works whitespace and case insensitiveley
	 */
	protected boolean safeContains(String s, Collection<String> c)
	{
		s=s.trim();
		Iterator<String> i = c.iterator();
		while (i.hasNext())
		{
			String next = i.next();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Comparing |"+next+"| with |"+s+"|");
			}
			if (next.trim().equalsIgnoreCase(s))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Given a model as a String, get everything in it that looks like a group name.  Note that we're not parsing the model, but just doing regex matching.
	 * This therefore has it's holes, but should be sufficent to cover the error cases this code is aimed at (bad LDAP connections and human error rather
	 * than deliberate obsfucation of the model)
	 * @param modelContent String representing model content
	 * @return Collection os the system names of groups of all types contained in the model
	 */
	public Collection<String> getGroupNamesFromModelContent(String modelContent)
	{
		Collection<String> rV = new ArrayList<String>(30);
		Matcher m = Pattern.compile(GROUP_NAME_REGEX).matcher(modelContent);
		while (m.find())
		{
			rV.add(m.group().substring(11)); // The length of "systemName="
		}
		return rV;
	}
	
}
