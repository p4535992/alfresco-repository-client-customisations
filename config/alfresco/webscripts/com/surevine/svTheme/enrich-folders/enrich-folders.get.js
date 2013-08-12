var enrichedFolders = new Array();

for (var i=0; i < argsM['folders'].length; i++) {
	logger.log("Enriching "+argsM['folders'][i]);
	var folder = utils.getNodeFromString(argsM['folders'][i]);
	try {
		var rV = {};
		rV.siteName=folder.getSiteShortName();
		rV.nodeRef=folder.getNodeRef();
		rV.name=folder.properties["cm:name"];
		rV.title=folder.properties["cm:title"];
		rV.description=folder.properties["cm:description"];
		rV.path=folder.displayPath.substr(folder.displayPath.indexOf("documentLibrary")+15); //Get the path within the doclib as opposed to the whole of alfresco
		enrichedFolders.push(rV);
	}
	catch (e) {
		logger.log("Could not enrich folder "+folder+" due to: "+e);
	}
}
model.folders = jsonUtils.toJSONString(enrichedFolders);