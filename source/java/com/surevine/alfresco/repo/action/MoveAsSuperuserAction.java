package com.surevine.alfresco.repo.action;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.surevine.alfresco.model.ManagedDeletionModel;

public class MoveAsSuperuserAction extends ActionExecuterAbstractBase {
	
	private static class MoveException extends RuntimeException {
		public MoveException(String s) {
			super(s);
		}
		public MoveException(String s, Throwable cause) {
			super(s, cause);
		}
	}
	
	private BehaviourFilter _policyFilter;
	public void setPolicyFilter(BehaviourFilter setFilter)
	{
		_policyFilter=setFilter;
	}	

	
    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(MoveAsSuperuserAction.class);

	private static final String PARAM_NAME="destination";
	private static final String PARAM_DISPLAY_NAME="Destination";
	private static final String PARAM_RESULT="result";

	private CopyService _copyService; //Injected
	public void setCopyService(CopyService cs) {
		_copyService=cs;
	}
	
	private FileFolderService _fileService; //Injected
	public void setFileFolderService(FileFolderService ffs) {
		_fileService=ffs;
	}
	
	private NodeService _nodeService; //Injected
	public void setNodeService(NodeService ns) {
		_nodeService=ns;
	}
		
	private TransactionService _transactionService; //Injected
	public void setTransactionService(TransactionService ts) {
		_transactionService=ts;
	}
	
	/**
	 * The "delete" part of this action runs as a super-user, which can be injected or takes the default value "admin".  
	 * This is so that we can delete items as part of a move where we would not usually have permissions
	 * @param userName ID of the super-user
	 */
	public void setRunAsUser(String userName)
	{
		_runAsUser=userName;
	}
	private String _runAsUser="admin";
	

	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {
		//This is all magic boilerplate to get the doMove method into a new, non-read-only, transaction
		_transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Boolean>(){
		    public Boolean execute() throws Throwable {
		    	NodeRef movedNodeRef = doMove(action, nodeRef);
		    	action.setParameterValue(PARAM_RESULT, movedNodeRef.toString());
		    	return Boolean.TRUE;
		    }}, false, true);
	}
	
	/**
	 * Works like this:
	 *   1)  Run some checks on the destination
	 *   2)  Copy the file to it's destination as permission check
	 *   3)  Delete the copied file, as a god user
	 *   4)  Move the file, as god user
	 *   5)  Return node reference of new file
	 */
	public NodeRef doMove(final Action action, final NodeRef nodeRef) {
		try {
		
			if (logger.isDebugEnabled()) {
				logger.debug("Moving "+nodeRef+" to "+(NodeRef)(action.getParameterValue(PARAM_NAME)));
			}
			 _policyFilter.disableBehaviour(ManagedDeletionModel.ASPECT_FOLDER_MARKED_FOR_DELETION);
			
			NodeRef destination =  (NodeRef)(action.getParameterValue(PARAM_NAME));
			runValidation(nodeRef, destination);
			
			// Attempt to copy node as permissions check
			NodeRef tmpNodeRef = null;
			ChildAssociationRef assocRef = null;
			try {
				
				assocRef = _nodeService.getPrimaryParent(nodeRef);
				
				tmpNodeRef = _copyService.copyAndRename(
		                nodeRef,
		                destination,
		                assocRef.getTypeQName(),
		                assocRef.getQName(),
		                true);
				
			} catch (FileExistsException e) {
				logger.error("The item already exists in ("+destination+")", e);
				throw new MoveException("The item already exists in ("+destination+")", e);
			} catch (DuplicateChildNodeNameException e) {
				logger.error("The item or child already exists in ("+destination+")", e);
				throw new MoveException("The item or child already exists in ("+destination+")", e);
			} catch (Exception e) {
				logger.error("An unexpected exception occured when attempting to copy the file to"+destination, e);
				throw new MoveException("An unexpected exception occured when attempting to copy the file to"+destination, e);
			}
			
			// Delete temporary node
			AuthenticationUtil.runAs(new DeleteWork(tmpNodeRef), _runAsUser);
			
			// Ensure deletion was successful
			if (_nodeService.exists(tmpNodeRef)) {
				logger.error("The node "+tmpNodeRef+" should have been deleted, but still exists.");
				throw new MoveException("The node "+tmpNodeRef+" should have been deleted, but still exists.");
			}
			
			// Move node
			NodeRef newNodeRef = AuthenticationUtil.runAs(new MoveWork(nodeRef, destination, assocRef.getQName().getLocalName()), _runAsUser);
			
			// Ensure that file still exists following move
			if(!_nodeService.exists(newNodeRef)) {
				logger.error("The moved item ("+newNodeRef+") could not be located at destination ("+destination+").");
				throw new MoveException("The moved item ("+newNodeRef+") could not be located at destination ("+destination+").");
			}
	
			return newNodeRef;
		}
		catch (Exception e) {
			logger.error("Failed to execute action: "+e, e);
			if (e instanceof MoveException) {
				throw (MoveException)e;
			}
			throw new MoveException("Failed to execute action", e);
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> params) {
		params.add(new ParameterDefinitionImpl(PARAM_NAME, DataTypeDefinition.NODE_REF, true, PARAM_DISPLAY_NAME));
	}
	
	/**
	 * Perform various checks on move destination to prevent undesired actions
	 * @param nodeRef
	 * @param destination
	 */
	protected void runValidation(NodeRef nodeRef, NodeRef destination) {
		
		// Prevent moving to current location
		ChildAssociationRef childAssociationRef = _nodeService.getPrimaryParent(nodeRef);
		NodeRef parentNodeRef = childAssociationRef.getParentRef();
		
		if(parentNodeRef.equals(destination)) {
			throw new MoveException("The file already exists in ("+destination+")");
		}
		
		// Prevents moving outside of current site document library
		Path currentSitePath = _nodeService.getPath(nodeRef).subPath(4);
		Path destinationSitePath = _nodeService.getPath(destination).subPath(4);
		
		if(!destinationSitePath.equals(currentSitePath)) {
			throw new MoveException("("+destination+") is not a valid destination.");
		}
		
	}
	
	private class DeleteWork implements RunAsWork<Boolean>
	{
		private NodeRef _nodeRef;
		public DeleteWork(NodeRef nodeRef)
		{
			_nodeRef=nodeRef;
		}
		
		@Override
		public Boolean doWork() throws Exception {
			_fileService.delete(_nodeRef);
			return true;
		}	
	}
	
	private class MoveWork implements RunAsWork<NodeRef>
	{
		private NodeRef _sourceNodeRef;
		private NodeRef _targetParentNodeRef;
		private String _fileName;
		public MoveWork(NodeRef sourceNodeRef, NodeRef targetParentNodeRef, String fileName)
		{
			_sourceNodeRef=sourceNodeRef;
			_targetParentNodeRef=targetParentNodeRef;
			_fileName=fileName;
		}
		
		@Override
		public NodeRef doWork() throws Exception {
			NodeRef newNodeRef = _fileService.move(_sourceNodeRef, _targetParentNodeRef, _fileName).getNodeRef();
			return newNodeRef;
		}	
	}
	
}
