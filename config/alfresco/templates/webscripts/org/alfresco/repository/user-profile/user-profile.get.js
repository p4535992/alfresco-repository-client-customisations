/**
 * Server-side logic to retrieve information on a user's profile from the repository
 */

/**
 * Retrieve profile information for a given user.
 * @param user Person object (ie. not a user name) of the user to retrieve details for
 */
function getProfileForUser(user)
{
	var profile = new Object();
	if (user.assocs["cm:avatar"]) {
		profile.avatar=user.assocs["cm:avatar"][0];
	}
		
	profile.userName = sanitiser.sanitise(user.properties["cm:userName"]);
	profile.firstName = sanitiser.sanitise(user.properties["cm:firstName"]);
	profile.lastName = sanitiser.sanitise(user.properties["cm:lastName"]);
	profile.biography = sanitiser.sanitise(user.properties["up:biography"]);
	
	profile.presence = presenceService.getUserPresence(user.properties["cm:userName"]);
	
	profile.askMeAbouts = user.properties["up:askMeAbouts"];
	var sanitisedAMA = [];
	for (var i=0; profile.askMeAbouts!=null && i < profile.askMeAbouts.length; i++)
	{
		sanitisedAMA.push(sanitiser.sanitise(profile.askMeAbouts[i]));
	}
	profile.askMeAbouts=sanitisedAMA;
	
	profile.email = sanitiser.sanitise(user.properties["cm:email"]);
	profile.telephones = [];
	profile.organisation = sanitiser.sanitise(getOrganisationFromUserName(user.properties["cm:userName"]));
	
	var rawTels = user.properties["up:telephoneNumbers"];
	
	//If we've got any telephone numbers, they MUST contain three fields seperated by commas.  It's OK for any of these
	//fields to be empty, but the commas must still be present
	if (rawTels!=null)
	{
		for (var i=0; i < rawTels.length; i++)
		{
			var tel = new Object();
			var numberParts = rawTels[i].split(",");
			tel.number=sanitiser.sanitise(numberParts[0]);
			tel.network=sanitiser.sanitise(numberParts[1]);
			tel.extension=sanitiser.sanitise(numberParts[2]);
			profile.telephones.push(tel);
		}
	}
	
	//Correct any null or empty values
	if (profile.biography==null)
	{
		profile.biography="A biography for this user has not been recorded";
	}
	if (profile.askMeAbouts==null)
	{
		profile.askMeAbouts=[];
	}
		
	return profile;
}

/**
 * Simple function to interrogate the arguments and work out if edit mode is requested.
 * Note that there's nothing to check that the calling user is allowed to edit a given profile, yet.
 * @return True if the "mode" argument is "edit" (case sensitive) - false otherwise.  Always returns "true"
 * for admin users
 */
function getEditMode()
{
	if (people.isAdmin(person))
	{
		return true;
	}
	var editModeArg = args.mode;
	if (editModeArg!=null && editModeArg=='edit')
	{
		return true;
	}
	return false;
}

/**
 * Assuming that user names are in the format [user]-[organisation], return a human-friendly version of the 
 * [organisation] component
 * @param userName String value - the full user name
 * @return A String representing the full name of the organisation
 */
function getOrganisationFromUserName(userName)
{
	var userNameParts = userName.split("-");
	var organisationMapping = new Object();
	
	//We assume that these two arrays are of the same length and that items with the same index relate to each other
	var organisationKeys = new Array(@@organisation_short_names@@);
	var organisationValues = new Array(@@organisation_long_names@@);
	for (var i=0; i < organisationKeys.length; i++)
	{
		organisationMapping[organisationKeys[i]]=organisationValues[i];
	}

	if (userNameParts.length<2)
	{
		return "Unknown";
	}
	var rV = organisationMapping[userNameParts[1]];
	if (rV==null)
	{
		return "Unknown";
	}
	return rV;
}

/**
 * Return the url of a stylesheet to use based on the "style" argument.  Either simply return the value of the argument
 * or, if it is not present, return a default value.  Note that this is a link back to the repo without passing 
 * through the share proxy, which might have implications for firewalls etc
 * @return
 */
function getStyleSheet()
{
	if (args.style==null || args.style=="default" || args.style=="")
	{
		return url.context+"/css/profile/default.css";
	}
	else {
		return args.style;
	}
}

/**
 * Convenience function to get the current user's profile
 */
function getProfileForCurrentUser()
{
	return getProfileForUser(person); //"person" is a root-scoped object provided by alfresco
}

/**
 * Examine the arguments and return a user's profile.  Get the profile of the user specified in the "userName" argument
 * or, if none is specified, get the current user's profile.
 * @return
 */
function getProfileFromArguments()
{
	var argUserName = args.userName;
	if (argUserName==null || argUserName=="")
	{
		return getProfileForCurrentUser();
	}
	return getProfileForUser(people.getPerson(argUserName));
}

var profile = getProfileFromArguments();
model.profile=profile;
model.editMode=getEditMode();
model.style=getStyleSheet();
model.searchBase="@@ama_search_base@@"