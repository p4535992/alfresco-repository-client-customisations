
try {
	var jsonData = jsonUtils.toObject(requestbody.content);
	
	var reason = null;
	
	if(jsonData.reason) {
		reason = jsonData.reason;
	}
	
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
      managedDeletion.setPerishable(search.luceneSearch('+PATH:"' + nodePath + '"')[0], reason);
    }
  }
  else
  {
    for each (ref in argsM['nodeRef'])
    {
    	managedDeletion.setPerishable(search.findNode(ref), reason);
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
