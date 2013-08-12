package com.surevine.alfresco.repo;

import java.net.URLDecoder;
import java.util.Iterator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.Path.Element;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ISO9075;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NodeFinder {
	
	private static final StoreRef STORE_REF = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private static final String CONTENT_MODEL_URL = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}";

	private static final Log _logger = LogFactory.getLog(NodeFinder.class);

	/* Dictates how calls to NodeRef manage case sensitivity */
	public enum SearchCase { CASE_SENSITIVE, CASE_INSENSITIVE};

	
	protected SearchService _searchService; //Injected
	public void setSearchService(SearchService ss) {
		_searchService=ss;
	}
	
	protected NodeService _nodeService; //Injected
	public void setNodeService(NodeService ns) {
		_nodeService=ns;
	}
	
	  /**
		 * Searches for a node within the repository using the Lucene search. Allows the caller to indicate the case sensitivity of searching.
		 * 
		 * First tries a full path case sensitive search; if that returns no results uses a name based query (insensitive).
		 * 
		 * @param path the path to the item we wish to find
		 * @param searchCase - an enum indicating how to treat case when searching. 
		 * @return
		 */
		public NodeRef getNodeRef(final Path path, final SearchCase searchCase)
		{
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Running a NodeRef search for: "+getShortPathString(path));
			}
			
		
			NodeRef match = null;
		
			// first use the case-sensitive query since its quicker
			match = getNodeRefCaseSensitive(path);
			
			if (match ==  null && searchCase.equals(SearchCase.CASE_INSENSITIVE))
			{
				// no case-sensitive match; now try insensitive.
				match = getNodeRefCaseInsensitive(path);
			}
			
			if (_logger.isDebugEnabled())
			{
		    	String exitMessage = (match!=null)?"Found a NodeRef match for: ": "Failed to find a NodeRef match for: ";
					_logger.debug(exitMessage+getShortPathString(path));
			}
		
			return match;
			
		}
	
	/*
	 * Don't quite understand why this isn't in one of the core Alfresco services, but I've checked and it isn't.  Retrieve the node in the repository
	 * at the given path using lucene PATH query to locate the node.  Usually returns null if there is no node at the path but there seems to be a bug in Alfreso's 
	 * handling of lucene that under some conditions can cause this search to return the store root.  Our call to getShortPathString works
	 * around this bug.
	 * 
	 * Also the PATH query is case-sensitive. So a null result may mean that a document could exist, but with a different case in its name, or the folder path.
	 * 
	 * @see #getNodeRefCaseInsensitive(Path) for alternate name based query
	 * @author - simonw
	 * @param path Path to a node
	 * @return NodeRef representing the same node as the input path
	 */
	protected NodeRef getNodeRefCaseSensitive(Path path)
	{
		//For some reason I can't quite work out the search fails with long paths, so I use the short versions here
	    ResultSet rs = _searchService.query(STORE_REF, SearchService.LANGUAGE_LUCENE, "PATH:\""+getShortPathString(path)+"\"");
	 
	    NodeRef match = null;
	    
	    try
	    {
	        if (rs.length() > 0)
	        {
	        	match = rs.getNodeRef(0);
	        }
	    }
	    finally
	    {
	        rs.close();
	    }
	    
	    return match;
	}
	
	/**
	 * In order to work around what seems to be a bug in Alfresco's handling of lucene, this method takes a path in the alfresco repository and swaps 
	 * out long namespaces with shortened versions for the app, st and cm prefixes
	 * @param path
	 * @return
	 */
    public String getShortPathString(Path path)
    {
    	final StringBuilder pathBuilder = new StringBuilder();
    	final Iterator<Element> elements = path.iterator();
    	
		while (elements.hasNext()) // Parse the Path to a String, encoding any content.
		{
			final Element element = elements.next();
			final String elementStr = element.getElementString();
			
			if (pathBuilder.length() > 1) pathBuilder.append('/');
			
			if (elementStr.startsWith(CONTENT_MODEL_URL))
			{
				pathBuilder.append("cm:");
				// get decoded value; prevents double encoding
				final String elementValue = ISO9075.decode(getElementValue(element));
				pathBuilder.append(ISO9075.encode(elementValue));
			} else {
				pathBuilder.append(elementStr);
			}
		}
    	
        final String pathString = URLDecoder.decode(pathBuilder.toString());
        
        if (pathString.lastIndexOf("/{") == -1) //Long form path
        {
            //Short form path, so just return input path - we should probably never have called this method anyway
            return path.toString();
        }
        
        return pathString.replaceAll("\\{http://www.alfresco.org/model/application/1.0\\}", "app:")
            .replaceAll("\\{http://www.alfresco.org/model/site/1.0\\}", "st:");
    }
    
    /**
	 * A case insensitive lookup of a NodeRef. Uses Lucene searching on the name property. 
	 * Compares all the resulting paths in a case-insensitive manner. This is used since
	 * a lucene search using the PATH term is case-sensitive. For the purposes of deletion
	 * we treat the item  (including its path) case-insensitive.
	 * 
	 * @param lookupPath Path to a node
	 * @return NodeRef representing the same node as the input path
	 */
	protected NodeRef getNodeRefCaseInsensitive(final Path lookupPath)
	{
		
		final String matchedString = getShortPathString(lookupPath);
		
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Running a case-insensitive search for: "+ matchedString);
		}
		
		NodeRef match = null;
		
		// get the name of the node
		final String nameStr= getElementValue(lookupPath.last());
		String searchStr = "@cm\\:name:\"" + nameStr + "\"";
	
		
	    ResultSet rs = _searchService.query(STORE_REF, SearchService.LANGUAGE_LUCENE, searchStr);
	    
	    try
	    {
	        if (rs.length() > 0)
	        {
	            /* process results checking paths match against our lookupPath
	             * we have to use the shortPath version since this handles space encoding appropriately
	             */
	        	for (NodeRef ref : rs.getNodeRefs())
	        	{
	        		final Path path = _nodeService.getPath(ref);
	        		final String pathStr = getShortPathString(path);
	        		
	        		
	        		if (matchedString.equalsIgnoreCase(pathStr))
	        		{
	        			match = ref;
	        			break;
	        		}
	        	}
	        }
	        
	    }
	    finally
	    {
	        rs.close();
	    }
	    
	    if (_logger.isDebugEnabled() )
		{
	    	String exitMessage = (match!=null)?"Found a case-insensitive match for: ": "No case-insensitive match for: ";
			_logger.debug(exitMessage+matchedString);
		} 
		
	    
	    return match;
	}
    
	/**
	 * Retrieves some value from path element.
	 * Assumes that the element string will always be in long format.
	 * 
	 * @param element the path element.
	 * @return the empty string or the value encoded.
	 */
	public String getElementValue(final Element element)
	{
		String result = StringUtils.EMPTY;
		final String elementString = element.getElementString();
		if (elementString.startsWith("{http"))
		{
			result =elementString.substring(elementString.indexOf("}")+1);
		}
		
		return result;
	}
	
	/**
	 * Retrieves some value from path element ISO9075 Encoded.
	 * Assumes that the element string will always be in long format.
	 * 
	 * @param element the path element.
	 * @return the empty string or the value encoded.
	 */
	public String getISOEncodedValue(final Element element)
	{
		return ISO9075.encode(getElementValue(element));
	}
	
	public String getName(final Path path) {
		String originalName = path.last().toString();
		if (originalName.indexOf("}")!=-1)
		{
			originalName=originalName.substring(originalName.indexOf("}")+1);
		}
		return originalName;
	}
	


}
