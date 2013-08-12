
var storeRef="workspace://SpacesStore";

if (args.tagNames!=null && args.tagNames.length>0)
{
	var tagNames = args.tagNames.split(",");
	var nodeRefs = [];
	
	logger.log("Found "+tagNames.length+" tags within ["+args.tagNames+"]");
	
	for (var i=0; i < tagNames.length; i++)
	{
		if (tagNames[i].replace(/^\s+|\s+$/g,"").length>0)
		{
			var tagName = tagNames[i].replace(/^\s+|\s+$/g,"");
			var existingNode = taggingService.getTag(storeRef, tagName);
			var tagObj = {};
			tagObj.name=tagName;
			if (existingNode != null)
			{
				nodeRefStr = existingNode.getNodeRef().toString(); //Don't need the toString but might as well be explicit
				logger.log("For tag with name: "+tagName+" retrieving existing nodeRef: "+nodeRefStr);
			}
			else //Node doesn't exist so create it
			{
				nodeRefStr = taggingService.createTag(storeRef, tagNames[i]).getNodeRef().toString();
				logger.log("For new tag with name: "+tagName+" creating new nodeRef: "+nodeRefStr);
			}
			tagObj.nodeRef=nodeRefStr
			nodeRefs.push(tagObj);
		}
	}
	
	logger.log("Returning "+nodeRefs.length+" nodeRefs");
	
	model.tags=nodeRefs;
}
else
{
	logger.log("tagNames parameter empty or not present")
	model.tags = [];
}