package com.surevine.alfresco.repo.action.delete;

import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A quartz job which wraps DeleteAction.  Runs a lucene search to identify items which require deletion then executes DeleteAction to delete them
 * @author simonw
 *
 */
public class DeleteActionJob implements Job {

	private static final Log _logger = LogFactory.getLog(DeleteActionJob.class);

	private static final StoreRef STORE_REF = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	private String
			_itemsToDeleteQuery,
			_itemsToDeleteFilterFailedQuery;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException 
	{
	    if (_logger.isDebugEnabled())
	    {
	    	_logger.debug("Running Delete Action Job...");
	    }
	    
		//Retrieve job data from the scheduled jobs context
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		
		Object actionServiceO = jobData.get("actionService");
	    if(actionServiceO == null || !(actionServiceO instanceof ActionService))
	    {
	       throw new AlfrescoRuntimeException(
	             "DeleteActionJob data must contain a valid 'actionService' reference");
	    }
	    ActionService actionService = (ActionService)actionServiceO;
	    
	    Object searchServiceO = jobData.get("searchService");
	    if(searchServiceO == null || !(searchServiceO instanceof SearchService))
	    {
	       throw new AlfrescoRuntimeException(
	             "DeleteActionJob data must contain a valid 'SearchService' reference");
	    }
	    SearchService searchService = (SearchService)searchServiceO;

	    _itemsToDeleteQuery = jobData.getString("itemsToDeleteQuery");
	    _itemsToDeleteFilterFailedQuery = jobData.getString("itemsToDeleteFilterFailedQuery");

	    if (_logger.isDebugEnabled())
	    {
	    	_logger.debug(String.format("Items to delete lucene query: %s .", _itemsToDeleteQuery));
	    }
	    if (_logger.isDebugEnabled())
	    {
	    	_logger.debug(String.format("Items to delete excluding failed lucene query: %s .", _itemsToDeleteFilterFailedQuery));
	    }
	    
	    // Create an Action based on the DeleteAction ActionExecutor
	    if (_logger.isDebugEnabled())
	    {
	    	_logger.debug("Creating action instance");
	    }
	    Action actionImpl = actionService.createAction(DeleteAction.NAME);
	    
	    
	    if (_logger.isDebugEnabled())
	    {
	    	_logger.debug("Finding items due for archival");
	    }
	    
	    ResultSet rs = null;
	    try {
		    //Find all items to expire
		    rs = searchService.query(STORE_REF, SearchService.LANGUAGE_LUCENE, assembleLuceneQuery());
		    Iterator<ResultSetRow> results = rs.iterator();
		    if (_logger.isDebugEnabled())
		    {
		    	_logger.debug("Found "+rs.length()+" items to archive");
		    }
		    
		    while (results.hasNext())
		    {
		    	NodeRef target = results.next().getNodeRef();
		    	if (_logger.isInfoEnabled())
		    	{
		    		_logger.info("Archiving "+target);
		    	}
		    	try {
		    		AuthenticationUtil.runAs( new ExecuteJobWork(actionImpl, target, actionService), AuthenticationUtil.getSystemUserName());
		    	}
		    	catch (Throwable e)
		    	{
		    		if (e.getCause()!=null)
		    		{
		    			e=e.getCause();
		    		}
		    		_logger.warn("Could not archive "+target+" due to a "+e, e);
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
	}
	
	private class ExecuteJobWork implements RunAsWork<Boolean>
	{
		private Action _action;
		private NodeRef _target;
		private ActionService _actionService;
		public ExecuteJobWork(Action action, NodeRef target, ActionService actionService)
		{
			_action=action;
			_target=target;
			_actionService=actionService;
		}
		public Boolean doWork() throws Exception {
    		_actionService.executeAction(_action, _target); //Delete the item - we log and ignore errors to prevent one corrupt item from stopping the entire archival run
    		return Boolean.TRUE;
		}
	}
	
	/**
	 * Separated into a separate method to allow for optimisation etc.
	 * @return
	 */
	protected String assembleLuceneQuery()
	{
		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		
		// Attempt to delete items that have previously failed to delete if the
		// day of the week is a saturday and it's between midnight and 1am.
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && calendar.get(Calendar.HOUR_OF_DAY) == 0) {
			if (_logger.isDebugEnabled()) {
				_logger.debug("Not filtering items that have failed to delete.");
			}
			return _itemsToDeleteQuery;
		} else {
			if (_logger.isDebugEnabled()) {
				_logger.debug("Filtering items that have failed to delete.");
			}
			return _itemsToDeleteFilterFailedQuery;
		}
	}
}
