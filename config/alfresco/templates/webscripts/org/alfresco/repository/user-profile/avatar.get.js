//Redirects to the logo for the given organisation.  If the organisation cannot be found, an exception will be thrown
function redirectToOrganisation(organisationName, size)
{
	logger.log("Looking for the logo for organisation: "+organisationName);
	var imageNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Organisation Logos/'+organisationName+'.png');
	if (imageNode != null) 
	{
		status.code=302;
		var nr = imageNode.nodeRef;
		status.location="/alfresco/wcservice/api/node/"+nr.toString().replaceAll("://","/")+"/content/thumbnails/"+size+"?c=force";
	}
	else
	{
		throw "No organisation logo found for "+organisationName;
	}
}

var size = args.size;
//If size isn't set we use the sensible default of "avatar", which is a 64x64 png that will attempt to keep an aspect ratio and trim
if (size == null)
{
	size="avatar";
}

var specifiedUserName = args.user;
var personNode = people.getPerson(specifiedUserName);
var avatar=null;
if (personNode.assocs["cm:avatar"]) {
	avatar=personNode.assocs["cm:avatar"][0]; //Alfresco ensures there's only ever one avatar for a given user, so the [0] is OK
}

//If an avatar has been set for the user, use that
if ( avatar != null )
{
	status.code=302; 
	status.location="/alfresco/wcservice/api/node/"+avatar.nodeRef.toString().replaceAll("://","/")+"/content/thumbnails/"+stringUtils.urlEncode(size)+"?c=force";
}
else //avatar is null, so redirect to a profile image for the organisation, if one is set 
{
	try 
	{
		var organisationName = specifiedUserName.substr(specifiedUserName.indexOf('-')+1);
		redirectToOrganisation(organisationName, size);
	}
	catch (e)
	{
		//We couldn't find a profile image, so log and redirect to the emergency-backup silhouette, which is in the usual place but called "notfound.png"
		logger.log("Could not retrieve an organisation-based profile image for "+specifiedUserName+": "+e);
		redirectToOrganisation("notfound", size);
	}
}

//If we get to here, something has gone wrong, so the HTML page we end up at is an error message.  But it shouldn't actually be possible to get here anyway