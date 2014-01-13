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

//Redirects to the logo for the given organisation.  If the organisation cannot be found, an exception will be thrown
function redirectToOrganisation(organisationName, size) {
	logger.log("Looking for the logo for organisation: "+organisationName);
	var imageNode = companyhome.childByNamePath('Data Dictionary/SV Theme/Organisation Logos/'+organisationName+'.png');
	if (imageNode != null) {
		status.code=302;
		var nr = imageNode.nodeRef;
		status.location = "/alfresco/wcservice/api/node/"
        +nr.toString().replaceAll("://","/") +"/content/thumbnails/" +size
        +"?c=force";
	} else {
		throw "No organisat ion logo found for " +organisationName;
	}
}

var size = args.size;
// If size isn't set we use the sensible default of "avatar", which is a 64x64
// png that will attempt to keep aspect ratio and trim.
if (size == null) {
	size="avatar";
}

var specifiedUserName = args.user;

if (specifiedUserName.substring(0, 10) == "talk-admin") { // If it's a group chat from admin
  specifiedUserName = "admin";
} else if (specifiedUserName.substring(0, 5) == "talk-") { // If it's a group chat from a username in the standard format
  specifiedUserName = specifiedUserName.substring(5).match(/\w+-\w+/)[0];
} // Else we assume it's an individual chat and we should have the avatar.

var personNode = people.getPerson(specifiedUserName);

var avatar;
if (personNode.assocs && personNode.assocs["cm:avatar"]) {
	avatar = personNode.assocs["cm:avatar"][0]; //Alfresco ensures there's only ever one avatar for a given user, so the [0] is OK
}

//If an avatar has been set for the user, use that
if (!avatar) {
	status.code = 302; 
	status.location = "/alfresco/wcservice/api/node/"+avatar.nodeRef.toString().replaceAll("://","/")+"/content/thumbnails/"+stringUtils.urlEncode(size)+"?c=force";
} else { // avatar is null, so redirect to a profile image for the organisation, if one is set
	try {
		var organisationName = specifiedUserName.substr(specifiedUserName.indexOf('-')+1);
		redirectToOrganisation(organisationName, size);
	} catch (e) {
		//We couldn't find a profile image, so log and redirect to the emergency-backup silhouette, which is in the usual place but called "notfound.png"
		logger.log("Could not retrieve an organisation-based profile image for "+specifiedUserName+": "+e);
		redirectToOrganisation("notfound", size);
	}
}

//If we get to here, something has gone wrong, so the HTML page we end up at is an error message.  But it shouldn't actually be possible to get here anyway
