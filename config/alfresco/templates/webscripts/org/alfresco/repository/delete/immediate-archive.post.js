<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

try {

  if (!isDeleter())
  {
    throw "The current user is not authorised to immediatley delete this item";	  
  }
	
  var action = actions.create("com.surevine.alfresco.repo.action.delete.DeleteAction");
  
  
  if (args.path)
  {
	for each (path in argsM['path'])
	{
	  //change:  /app:company_home/st:sites/cm:sandbox/cm:wiki/cm:1a
	  // into: /app:company_home/st:sites/cm:sandbox/cm:wiki/cm:_x0031_a (or whatever ISO 9075 is for a '1'
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