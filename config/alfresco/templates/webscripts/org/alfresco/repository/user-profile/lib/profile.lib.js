
/**
 * Set the user's profile data
 * @param data
 * @return
 */
function setProfileData(data)
{
	var user = people.getPerson(data.userName);
	user.properties["up:biography"]=data.biography;
	user.properties["up:askMeAbouts"]=data.askMeAbouts;
	user.properties["up:telephoneNumbers"]=data.telephones;
	user.properties["up:lastModified"]= new java.util.Date();
	if (data.organisation) {
		user.properties["cm:organizationId"]=data.organisation;
	}
	user.save();
}

function setAvatar(userID, content, filename) //Lots of this is copied from the OOB uploadavatar.post.js webscript
{
	var user = people.getPerson(userID);
	
	// ensure cm:person has 'cm:preferences' aspect applied - as we want to add the avatar as
    // the child node of the 'cm:preferenceImage' association
    if (!user.hasAspect("cm:preferences")) {
       user.addAspect("cm:preferences");
    }

    // remove old image child node if we already have one
    var assocs = user.childAssocs["cm:preferenceImage"];
    if (assocs != null && assocs.length == 1)
    {
       assocs[0].remove();
    }

    // create the new image node
    var image = user.createNode(filename, "cm:content", "cm:preferenceImage");
    image.properties.content.write(content);
    image.properties.content.guessMimetype(filename);
    image.properties.content.encoding = "UTF-8";
    image.save();

    // wire up 'cm:avatar' target association - backward compatible with JSF web-client avatar
    assocs = user.associations["cm:avatar"];
    if (assocs != null && assocs.length == 1)
    {
       user.removeAssociation(assocs[0], "cm:avatar");
    }
    user.createAssociation(image, "cm:avatar");
    return image.nodeRef;
}

/**
 * Parse the posted data into an object representation.
 * 
 * Example input data:
 * 
 * {
 *	"biography":"Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
 *	"askMeAbouts": ["Vivamus dignissim quam elit, at", "Nullam est. Vestibulum", "Nulla mattis convallis tempus"],
 *	"telephones": [ 
 *		{"number":"01257 54898547","network":"PSTN","extension":""},
 *		{"number":"97653","network":"FLIM","extension":"5897"},
 *		{"number":"5145","network":"FLAM","extension":""}
 *	]
 * }
 * 
 * @return
 */
function parseJsonData()
{
	var data = new Object();
	data.userName = sanitiser.sanitise(json.getString("userName"));
	data.biography = sanitiser.sanitise(json.getString("biography"));
	if (json.has("organisation")) {
		data.organisation = sanitiser.sanitise(json.getString("organisation"));
	}
	
	var askMeAboutObjs = [];
	var askMeAboutsRaw = json.getJSONArray("askMeAbouts");
	
	for (var i=0; askMeAboutsRaw!=null && i < askMeAboutsRaw.length(); i++)
	{
		var ama=sanitiser.sanitise(askMeAboutsRaw.getString(i));
		
		ama = ama.substr(0, 1024);	// The LDAP schema is limited to 1024 chars
		
		if (ama!=null && ama.length>0)
		{
			askMeAboutObjs.push(ama);
		}
	}
	data.askMeAbouts=askMeAboutObjs;
			
	var telephoneObjs=[];
	var telephoneRaw = json.getJSONArray("telephones");
	
	for (var i=0; telephoneRaw!=null && i < telephoneRaw.length(); i++) {
		var tel = new Object();
		var telephoneRawObj = telephoneRaw.getJSONObject(i);
		tel.number = sanitiser.sanitise(telephoneRawObj.getString("number"));
		tel.network = sanitiser.sanitise(telephoneRawObj.getString("network"));
		tel.extension = sanitiser.sanitise(telephoneRawObj.getString("extension"));
		if (tel.number!=null && tel.number.length()>0)
		{
			telephoneObjs.push(tel.number+","+tel.network+","+tel.extension);
		}
	}
	data.telephones=telephoneObjs;

	return data;
}