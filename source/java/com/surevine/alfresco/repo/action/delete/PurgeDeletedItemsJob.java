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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * A quartz job which wraps {@link DestroyAction}.  Runs a lucene search to identify items which require deletion then executes DeleteAction to delete them
 * @author simonw
 *
 */
public class PurgeDeletedItemsJob implements Job {

	private static final Log _logger = LogFactory.getLog(PurgeDeletedItemsJob.class);

	private static final StoreRef STORE_REF = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;

	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException 
	{
		
		//Retrieve job data from the scheduled jobs context
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		
		// Get the action service
		Object actionServiceO = jobData.get("actionService");
	    if(actionServiceO == null || !(actionServiceO instanceof ActionService))
	    {
	       throw new AlfrescoRuntimeException(
	             "DeleteActionJob data must contain a valid 'actionService' reference");
	    }
	    ActionService actionService = (ActionService)actionServiceO;

	    // Get the search service
	    Object searchServiceO = jobData.get("searchService");
	    if(searchServiceO == null || !(searchServiceO instanceof SearchService))
	    {
	       throw new AlfrescoRuntimeException(
	             "PurgeDeletedItemsJob data must contain a valid 'SearchService' reference");
	    }
	    SearchService searchService = (SearchService)searchServiceO;

	    // Get the node service
	    Object nodeServiceO = jobData.get("nodeService");
	    if(nodeServiceO == null || !(nodeServiceO instanceof NodeService))
	    {
	       throw new AlfrescoRuntimeException(
	             "PurgeDeletedItemsJob data must contain a valid 'NodeService' reference");
	    }
	    NodeService nodeService = (NodeService)nodeServiceO;
	    
	    // Get the configured number of days after which to purge items
		Object purgeDays0 = jobData.get("purgeDays");
	    if(purgeDays0 == null || !(purgeDays0 instanceof Integer))
	    {
	       throw new AlfrescoRuntimeException(
	             "PurgeDeletedItemsJob: 'purgeDays' must be a valid java.lang.Integer");
	    }
	    Integer purgeDays = (Integer) purgeDays0;
	    
	    
	    if (_logger.isInfoEnabled())
	    {
	    	_logger.info("Finding items which were deleted > " + purgeDays + " days ago to be destroyed");
	    }

	    Action actionImpl = actionService.createAction(DestroyAction.NAME);
	    actionImpl.setTitle("Purge Deleted Items Scheduled Job");
	    
		AuthenticationUtil.runAs( new ExecuteJobWork(actionImpl, actionService, searchService, nodeService, purgeDays), AuthenticationUtil.getSystemUserName());
	}
	
	private class ExecuteJobWork implements RunAsWork<Boolean>
	{
		private Action _action;
		private ActionService _actionService;
		private SearchService _searchService;
		private NodeService _nodeService;
		private int _purgeDays;
		
		public ExecuteJobWork(Action action, ActionService actionService, SearchService searchService, NodeService nodeservice, int purgeDays)
		{
			_action = action;
			_actionService = actionService;
			_searchService = searchService;
			_nodeService = nodeservice;
			_purgeDays = purgeDays;
		}
		
		public Boolean doWork() throws Exception {
		    ResultSet rs = null;
		    try {
			    //Find all items to expire
			    rs = _searchService.query(STORE_REF, SearchService.LANGUAGE_LUCENE, assembleLuceneQuery(_purgeDays));
			    Iterator<ResultSetRow> results = rs.iterator();
			    if (_logger.isInfoEnabled())
			    {
			    	_logger.info("Found "+rs.length()+" items to destroy");
			    }
			    
			    int count = 0;
		    	long startTime = 0;
		    	
			    while (results.hasNext())
			    {
			    	NodeRef target = results.next().getNodeRef();
			    	
			    	try {
						if (_logger.isTraceEnabled()) {
							startTime = System.currentTimeMillis();
						}

					    ++count;
					    
				    	if(!_nodeService.exists(target)) {
				    		_logger.warn("Skipping non-existant node returned in purge deleted items query: " + target);
				    		continue;
				    	}
				    	
				    	if(!_nodeService.hasAspect(target, ManagedDeletionModel.ASPECT_DELETED)) {
				    		_logger.warn("Skipping non-deleted node returned in purge deleted items query: " + target);
				    		continue;
				    	}
				    	
				    	if (_logger.isInfoEnabled())
				    	{
				    		_logger.info(String.valueOf(count) + ": Destroying "+target);
				    	}
						    _actionService.executeAction(_action, target); //Delete the item - we log and ignore errors to prevent one corrupt item from stopping the entire archival run
				    	}
			    	catch (Throwable e)
			    	{
			    		if (e.getCause()!=null)
			    		{
			    			e=e.getCause();
			    		}
			    		_logger.warn("Could not destroy "+target+" due to a "+e, e);
			    	}
			    	
			    	if(_logger.isTraceEnabled()) {
			    		_logger.trace("Processed " + target + " in " + (System.currentTimeMillis() - startTime) + "ms");
			    	}
			    }
		    }
		    finally
		    {
		    	if (rs!=null)
		    	{
		    		rs.close();
		    	}
		    }

    		return Boolean.TRUE;
		}
	}
	
	/**
	 * Separated into a separate method to allow for optimisation etc.  
	 * @return
	 */
	protected String assembleLuceneQuery(final int purgeDays)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 0 - purgeDays);
		cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
		String luceneDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(cal.getTime());		
		
		String query = 
			"ASPECT:\"md:deleted\"" +
			" AND (@md\\:deletedTimestamp:[MIN TO " + luceneDate + "]" +
				" OR (ISNULL:\"md:deletedTimestamp\"" +
					" AND @cm\\:modified:[MIN TO " + luceneDate + "]" +
				")" +
			")";
		
		if(_logger.isDebugEnabled()) {
			_logger.debug(query);
		}
		
		return query;
	}
}
