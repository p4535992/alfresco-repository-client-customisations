package com.surevine.alfresco.repo.impl;

import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.repository.ContentService;
import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.action.SanitiseHTMLAction;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An implementation of HTMLIdentifier that implements the identification algorithm described by INT-128
 * This algorithm identifies a piece of content as HTML if any of the following are true
 * <ul>
 *  <li> The Mime Type of the content item, as identified by Alfresco, contains the String "htm" </li>
 *  <li> The name of the content item contains one of a configurable series of extensions, such as .html or .shtm</li>
 *  <li> The first (configurable) X characters of the content item contain one of a configurable list of pattens, such as &lt;html&rt; or &lt;script&rt;</li>
 * </ul>
 * @author SimonW
 *
 */
public class FilenameAndContentHTMLIdentifier implements HTMLIdentifier {
	
	private static final QName CONTENT_QNAME = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
	private static final QName NAME_QNAME = QName.createQName("http://www.alfresco.org/model/content/1.0}name");
	private static final Log LOGGER = LogFactory.getLog(FilenameAndContentHTMLIdentifier.class);
	
	private NodeRef _target=null;
	private ContentService _contentService;
	private NodeService _nodeService;
	
	private String[] _htmlExtensions;
	
	static {
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Loaded FilenameAndContentHTMLIdentifier");
		}
	}
	
	/**
	 * Constructor for factory and testing containers
	 * @param contentService
	 * @param nodeService
	 * @param exts
	 * @param contentFragments
	 * @param maxChars
	 */
	public FilenameAndContentHTMLIdentifier(ContentService contentService, NodeService nodeService, String[] exts, String [] contentFragments, int maxChars)
	{
		_contentService=contentService;
		_nodeService=nodeService;
		_htmlExtensions=exts;
		_htmlContent=contentFragments;
		_maxCharsToSearch=maxChars;
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Created a FilenameAndContentHTMLIdentifier using ["+_contentService+"|"+_nodeService+"|"+_htmlExtensions+"|"+_htmlContent+"|"+_maxCharsToSearch+"]");
		}
	}
	
	/**
	 * Default constructor intended for use by setter injection containers
	 */
	public FilenameAndContentHTMLIdentifier() { }
	
	/**
	 * Utility method - convert input Strings to lower case to make later matching faster
	 * @param in An Array of Strings
	 * @return Equal to the input array of String, but everything is lower case
	 */
	protected static String[] StringArrayToLowerCase(String [] in)
	{
		if (in==null)
		{
			return null;
		}
		
		String[] out = new String[in.length];
		for (int i=0; i < in.length; i++)
		{
			if (in[i]!=null)
			{
				out[i]=in[i].toLowerCase();
			}
			else
			{
				out[i]=null;
			}
		}
		return out;
	}
	
	public void setHTMLExtensions(String[] htmlExtensions)
	{
		_htmlExtensions=StringArrayToLowerCase(htmlExtensions);
	}
	
	private String[] _htmlContent;
	
	public void setHTMLContent(String[] htmlContent)
	{
		_htmlContent=StringArrayToLowerCase(htmlContent);
		validateHTMLContent();
	}
	
	private int _maxCharsToSearch;
	
	public void setMaxCharsToSearch(int maxCharsToSearch)
	{
		_maxCharsToSearch=maxCharsToSearch;
	}
	
	public void setNodeService(NodeService nodeService)
	{
		_nodeService=nodeService;
	}
	
	public NodeRef getTargetNodeRef() 
	{
		return _target;
	}
	
	/**
	 * Only lower-case alphanumerics, <, > and - to prevent accidentally escaping regexes elsewhere in the code.
	 * A more sophisticated approach could be used to ensure that regexes included here are escaped correctly,
	 * but this is not required for our use-case
	 */
	private void validateHTMLContent()
	{
		if (_htmlContent==null)
		{
			return;
		}
		for (int i=0; i < _htmlContent.length; i++)
		{
			if (!_htmlContent[i].matches("[a-z0-9<>-]+"))
			{
				throw new IllegalArgumentException("HTML Content Array contains "+_htmlContent[i]+" at index "+i+" which is not a valid value");
			}
		}
	}
	
	
	protected Pattern getHTMLContentRegularExpression()
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Getting HTMLContentRegex");
		}
		StringBuffer sb = new StringBuffer(_htmlContent.length*6);
		sb.append(".*(");
		for (int i=0; i < _htmlContent.length; i++)
		{
			sb.append("(<\\s*?").append(_htmlContent[i]).append("[\\s>])");
			if (i<_htmlContent.length-1)
			{
				sb.append("|");
			}
		}
		sb.append(").*");
		String rVal = sb.toString();
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Created RE: "+rVal);
		}
		return Pattern.compile(rVal, Pattern.DOTALL);
	}
	
	public boolean isTargetHTML() throws IllegalStateException 
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Is target HTML?");
		}
		if (_target==null)
		{
			throw new IllegalStateException("isTargetHTML called but target not set");
		}
		
		if (isHTMLByMimeType() || isHTMLByFileName() || isHTMLByContent())
		{
			if (LOGGER.isInfoEnabled())
			{
				LOGGER.info(_target+" is HTML");
			}
			return true;
		}
		
		else
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info(_target+" is not HTML");
		}
		return false;
		
		}
	
	/**
	 * Examine the first X chars of the file, and identify the file as HTML if is contains one of a series of Strings within
	 * these chars
	 * @return True if any of the Strings in _htmlContent are present within the first _maxCharsToSearch chars of the content
	 * of the target node
	 */
	protected boolean isHTMLByContent()
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Entering isHTMLByContent");
		}
		ContentReader cr = _contentService.getReader(_target, CONTENT_QNAME);
		
		if (cr==null)
		{
			if (LOGGER.isWarnEnabled())
			{
				LOGGER.warn("Target "+_target+" has no content reader");
			}
			return false;
		}
		
		String contentString = cr.getContentString(_maxCharsToSearch); //In theory, could have problems with massive content, but given our use-case I'm confident we'll be OK
		
		//no content == no HTML content
		if (contentString==null || _htmlContent==null)
		{
			if (LOGGER.isWarnEnabled())
			{
				LOGGER.warn("Target "+_target+" has no content");
			}
			return false;
		}
		
		contentString = contentString.toLowerCase();
		Pattern regex = getHTMLContentRegularExpression();
		
		if (regex.matcher(contentString).matches())
		{
			if (LOGGER.isInfoEnabled())
			{
				LOGGER.info(_target+" has HTML content");
			}
			return true;
		}
		LOGGER.info(_target+" has no HTML content");
		return false;
	}
	
	/**
	 * Does the mimetype of the content item (as recorded by Alfresco) indicate
	 * that the content item is HTML?
	 */
	protected boolean isHTMLByMimeType()
	{
		ContentAccessor ca = _contentService.getReader(_target, CONTENT_QNAME);
		if (ca==null) //If we don't have any content, it can't be HTML
		{
			return false;
		}
		String mimeString = ca.getMimetype();
		
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Mime Type of "+_target+" is "+mimeString);
		}
		
		if (mimeString!=null && mimeString.toLowerCase().contains("htm"))
		{
			if (LOGGER.isInfoEnabled())
			{
				LOGGER.info("Mime Type indicates that "+_target+" is HTML");
			}
			return true;
		}
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Mime type indicates that "+_target+" is not HTML");
		}
		return false;
	}
	
	/**
	 * Retrieve the name of the given node, or null if the node either doesn't have the name or is itself null
	 * @param node
	 * @return Name of the node
	 */
	protected String getNodeName(NodeRef node)
	{
		Object rVal =  _nodeService.getProperty(_target, NAME_QNAME);
		if (rVal==null)
		{
			return null;
		}
		
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Name of "+node+" is "+rVal);
		}
		
		return rVal.toString();
	}
	
	protected boolean isHTMLByFileName()
	{
		String name = getNodeName(_target);
		if (name==null || _htmlExtensions==null)
		{
			return false; //If we've got no filename, we can't be HTML
		}
		
		// Case insensitive comparison - if the filename contains .htm, .html etc then it's html
		// Note we use contains, not ends-with logic, so a file called test.html.doc.tmp will be
		// identified as HTML, as will test.htmlfish, but we add a . to the front so myhtml.doc will
		// not be reported as HTML
		name=name.toLowerCase();
		
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("Name of "+_target+" is "+name);
		}
		
		for (int i=0; i < _htmlExtensions.length; i++)
		{
			if (LOGGER.isInfoEnabled())
			{
				LOGGER.info("Looking for "+_htmlExtensions[i]+" in "+name);
			}
			if (name.indexOf("."+_htmlExtensions[i])!=-1)
			{
				if (LOGGER.isInfoEnabled())
				{
					LOGGER.info("File name indicates that "+_target+" is HTML");
				}
				return true;
			}
		}
		
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("File name indicates that "+_target+" is not HTML");
		}
		
		return false;
	}

	public void setTargetNodeRef(NodeRef target){
		_target=target;
	}
	
	public void setContentService(ContentService contentService)
	{
		_contentService=contentService;
	}
	
}
