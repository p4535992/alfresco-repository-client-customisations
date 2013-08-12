package com.surevine.alfresco.repo.action;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import java.util.List;
import com.surevine.alfresco.repo.HTMLSanitiser;
import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.HTMLIdentifierFactory;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action to sanitise HTML content items as they are uploaded
 * @author SimonW
 *
 */
public class SanitiseHTMLAction extends ActionExecuterAbstractBase {

	
	private static final QName CONTENT_QNAME = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
	private static final QName WORKING_COPY_ASPECT_QName=QName.createQName("{http://www.alfresco.org/model/content/1.0}workingcopy");
	
	private static final Log LOGGER = LogFactory.getLog(SanitiseHTMLAction.class);
	
	private HTMLIdentifierFactory _identifierFactory;
	private HTMLSanitiser _sanitiser;
	private ContentService _contentService;
	private NodeService _nodeService;
	
	
	/**
	 * Injected
	 * @param nodeService
	 */
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	/**
	 * Injected
	 */
	public void setHTMLIdentifierFactory(HTMLIdentifierFactory identifierFactory)
	{
		_identifierFactory=identifierFactory;
	}
	
	/**
	 * Injected
	 */
	public void setHTMLSanitiser(HTMLSanitiser sanitiser)
	{
		_sanitiser=sanitiser;
	}
	
	/**
	 * Injected
	 */
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
	@Override
	protected void executeImpl(Action action, NodeRef node) {
		
		if (!_nodeService.exists(node))
		{
			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug(node+" doesn't exist anymore.  Not processing");
			}
			return;
		}
		
		//Don't process for working copies - only apply to real documents
		if (_nodeService.hasAspect(node, WORKING_COPY_ASPECT_QName))
		{
			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug(node+" is a working copy.  Not processing");
			}
			return;
		}
		
		HTMLIdentifier identifier = _identifierFactory.getHTMLIdentifier();
		identifier.setTargetNodeRef(node);
		
		//Only do work if we're uploading HTML
		if (identifier.isTargetHTML())
		{
			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug("Target Node "+node+" is HTML");
			}
			//Get the sanitised content
			String existingContent = _contentService.getReader(node, CONTENT_QNAME).getContentString();
			String newContent = _sanitiser.sanitiseHTMLString(existingContent);
			
			//We'll close the output stream ourselves
			try 
			{
			  ContentWriter writer = _contentService.getWriter(node, CONTENT_QNAME, true);
			  writer.putContent(newContent);
			  if (LOGGER.isDebugEnabled())
			  {
				  LOGGER.debug("New Content Written");
			  }
			}
			//We need to background this to work around Alfresco issues with working copies etc
			//So any exception we rethrow will just be swallowed.  To be safest, we need to delete the node
			//if we can't sanitise it
			catch (Exception e)
			{
				LOGGER.error("Exception thrown during HTMLSanitisationAction.  Attempting to delete node "+node, e);
				_nodeService.deleteNode(node);
			}
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// No parameters
	}
	
}
