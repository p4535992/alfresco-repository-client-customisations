package com.surevine.alfresco.repo.action;

import java.util.Collection;
import java.util.List;

import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import com.surevine.alfresco.esl.impl.EnhancedSecurityModel;
import com.surevine.alfresco.esl.exception.EnhancedSecurityException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Behavior to provide customer-specific validation for Enhanced Security on top of that provided by the Alfresco data model
 * 
 * This class just checks that items in Alfresco Share have the relevant aspect applied, but provides a base for subclasses
 * to perform richer validation in the future.
 * 
 * Note that our aim is not to reproduce validation (eg. enumeration-checking) performed elsewhere
 * 
 * @author simonw
 *
 */
public class EnhancedSecurityCustomerSpecificValidation implements ContentServicePolicies.OnContentUpdatePolicy {
	
	private static final Log LOGGER = LogFactory.getLog(EnhancedSecurityCustomerSpecificValidation.class);

	/**
	 * String within an item's path immediately preceding the name of the site the item is in
	 */
	private static final String SITE_PART_OF_PATH="{"+SiteModel.SITE_MODEL_URL+"}sites/{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}";

	private static final String WIKI_PATH_PART="{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}wiki/{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}";

	private static final String DISCUSSION_PATH_PART="{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}discussions/{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}";
	
	private static final String DOCLIB_PATH_PART="{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}documentLibrary/{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}";


	private boolean _skipValidationOnAmbiguity=false;
	private PolicyComponent _policyComponent;
	private NodeService _nodeService;
	
	/**
	 * @param skip  If set to true, then do not run validation if we are unsure what sort of item a node represents.  Recommend setting to false for production
	 * and only setting to true on the advice of support.  Usually spring-injected
	 */
	public void setSkipValidationOnAmbiguity(boolean skip)
	{
		_skipValidationOnAmbiguity=skip;
	}
	
	/**
	 * Injected
	 * @param policyComponent
	 */
	public void setPolicyComponent(PolicyComponent policyComponent)
	{
		_policyComponent = policyComponent;
	}
	
	/**
	 * Injected
	 * @param nodeService
	 */
	public void setNodeService (NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	/**
	 * Register this behaviour with the policy component. 
	 */
	public void init()
	{
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Initialising Enhanced Security Customer Specific Validation bean");
		}
		
		/**
		 * Fire the policy when content is updated, at the end of the transaction, for content and discussions
		 */
        _policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                ContentModel.TYPE_CONTENT,
                new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        _policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                ForumModel.TYPE_FORUM,
                new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        _policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                ForumModel.TYPE_POST,
                new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        _policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                ForumModel.TYPE_TOPIC,
                new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        
        if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Enhanced Security Customer Specific Validation bean initialised");
		}
	}

	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) 
	{
		//Because we're firing post-transaction, it's possible for a node (say, a working copy) to no longer exist by the time
		//we are fired
		if (_nodeService.exists(nodeRef))
		{
			if (LOGGER.isTraceEnabled()) //In regular business, this will fire _a_lot_ so is set to trace
			{
				LOGGER.trace("Processing customer specific validation logic on "+nodeRef);
			}
			if (shouldBeSecured(nodeRef))
			{
				if (LOGGER.isDebugEnabled())
				{
					LOGGER.debug(nodeRef+" should be secured - validating...");
				}
				validateLabel(nodeRef); //Throws exceptions if label is not valid
			}
			else
			{
				if (LOGGER.isTraceEnabled())
				{
					LOGGER.trace(nodeRef+ " should not be secured");
				}
			}
		}
	}
	
	/**
	 * Should the given node be secured via enhanced security.
	 * This method gets called _all_the_time_ and so logs at trace to avoid dramatically slowing down a reference system set at DEBUG
	 * @param nodeRef
	 * @return
	 */
	protected boolean shouldBeSecured(NodeRef nodeRef)
	{
		String pathStr = getPathString(nodeRef);
		
		//Do not check thumbnails, they are protected by virtue of the document they live under
		if (_nodeService.getType(nodeRef).equals(ContentModel.TYPE_THUMBNAIL))
		{
			return false;
		}
		
		//We only care about things in sites
		if (pathStr.indexOf(SITE_PART_OF_PATH)==-1)
		{
			return false;
		}
		
		//If it's in the wiki, we care
		if (pathStr.indexOf(WIKI_PATH_PART)!=-1)
		{
			return true;
		}
		
		//If it's in the discussion forum, we care
		if (pathStr.indexOf(DISCUSSION_PATH_PART)!=-1)
		{
			return true;
		}
		
		//We are in the document library now, by process of elimination, but check anyway in case we've somehow fallen through the gaps
		if (pathStr.indexOf(DOCLIB_PATH_PART)!=-1)
		{
			//Comments on folders don't need security markings
			QName type = _nodeService.getType(nodeRef);
			if (type.equals(ForumModel.TYPE_POST) || type.equals(ForumModel.TYPE_TOPIC) || type.equals(ForumModel.TYPE_FORUM))
			{
				return !isCommentOnFolder(nodeRef);
			}
			return true;
		}
		
		
		//fallout, assume either true or false depending on configuration
		//Note that if _SKIP_ validation is TRUE, we return FALSE
		if (_skipValidationOnAmbiguity)
		{
			if (LOGGER.isWarnEnabled())
			{
				LOGGER.warn("Could not determine if "+nodeRef+" requires security logic validation.  Skipping validation");
			}
			return false;
		}
		else
		{
			if (LOGGER.isWarnEnabled())
			{
				LOGGER.warn("Could not determine if "+nodeRef+" requires security logic validation.  Validating as a fail-safe");
			}
			return true;
		}
	}
	
	/**
	 * Returns true if the given node ref, which must represent part of a discussion, is part of a discussion on a folder.
	 * If the given node is a forum, then returns true if it's parent is a folder.  Otherwise, goes up the tree recursively until it finds
	 * a forum.
	 * @param nodeRef
	 * @return true if the node is a discussion node and is part of a discussion on a folder, false otherwise
	 */
	private boolean isCommentOnFolder(NodeRef nodeRef)
	{
		QName type = _nodeService.getType(nodeRef);
		if (!(type.equals(ForumModel.TYPE_POST) || type.equals(ForumModel.TYPE_TOPIC) || type.equals(ForumModel.TYPE_FORUM)))
		{
			throw new EnhancedSecurityException("A non-discussion node ("+nodeRef+" with type "+_nodeService.getType(nodeRef)+") was passed to isCommentOnFolder");
		}

		if (type.equals(ForumModel.TYPE_FORUM) )
		{
			List<ChildAssociationRef> parentLinks = _nodeService.getParentAssocs(nodeRef);
			if (parentLinks.size()!=1)
			{
				throw new EnhancedSecurityException("The forum node "+nodeRef+" has "+parentLinks.size()+" parents, which is unexpected.  It should have exactly one");
			}
			return _nodeService.getType(parentLinks.get(0).getParentRef()).equals(ContentModel.TYPE_FOLDER); //Is the parent a folder?

		}
		else
		{
			List<ChildAssociationRef> parentLinks = _nodeService.getParentAssocs(nodeRef);
			if (parentLinks.size()>0)
			{
				if (parentLinks.size()>1)
				{
					throw new EnhancedSecurityException("The discussion node "+nodeRef+" has "+parentLinks.size()+" parents, which is unexpected.  It should have at most one");
				}
				//Exactly 1 parentLink
				return isCommentOnFolder(parentLinks.get(0).getParentRef());
			}
		}
		//Fallout - will occur if we get all the way to the top of the tree and still can't find a forum
		return false;
	}
	
	/**
	 * Validate the security label on the given NodeRef
	 * @param nodeRef
	 * @throws EnhancedSecurityException if validation fails
	 */
	protected void validateLabel(NodeRef nodeRef)
	{
		checkAspectApplied(nodeRef, EnhancedSecurityModel.ASPECT_SECURITY_MARKING);
	}
	
	protected void checkAspectApplied(NodeRef nodeRef, QName aspect)
	{
		if (!_nodeService.hasAspect(nodeRef, aspect))
		{
			throw new EnhancedSecurityException(nodeRef+" did not have the mandatory aspect "+aspect);
		}
	}
	
	/**
	 * We don't use this method right now, but it is included as a convenience for subclasses
	 * @param nodeRef
	 * @param property
	 */
	protected final void checkStringPropertyNotEmpty(NodeRef nodeRef, QName property)
	{
		String propVal = (String)(_nodeService.getProperty(nodeRef, property));
		if (propVal==null)
		{
			throw new EnhancedSecurityException(nodeRef+" has a null value for mandatory property "+property); 
		}
		//else
		
		if (StringUtils.isBlank(propVal))
		{
			throw new EnhancedSecurityException(nodeRef+" has an empty value for mandatory property "+property); 
		}
	}
	
	private String getPathString(NodeRef nodeRef)
	{
		return _nodeService.getPath(nodeRef).toString();
	}
	
	
}