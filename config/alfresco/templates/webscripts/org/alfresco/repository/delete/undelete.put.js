<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

try {
	
  if (!isDeleter())
  {
    throw "The current user is not authorised to undelete this item";	  
  }
	
  var action = actions.create("com.surevine.alfresco.repo.action.delete.UndeleteAction");
  
  
  if (args.path)
  {
    for each (path in argsM['path'])
	{
      var indexOfLeafPart = path.lastIndexOf(':') + 1;
      var partPart = path.substring( 0, indexOfLeafPart);
      var leafPart = path.substring( indexOfLeafPart);
      var nodePath = partPart + search.ISO9075Encode(leafPart);
	  action.execute(search.luceneSearch('+PATH:"' + nodePath + '"')[0]);
    }
  }
  else 
  {
	for each (ref in argsM['nodeRef'])
	{
	  action.execute(search.findNode(ref));
    }	  
  }

  model.success=true;
}
catch (err)
{
  model.success=false;
  status.code=500;
  logger.log(err);
}