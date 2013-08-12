var users = argsM['users'];
logger.log("UsersA: "+users);

if (!users || users==null || users=="" || users==[]) {
        users=[];
        users.push(person.properties.userName);
}

logger.log("UsersB: "+users);

var presences = [];
for each (user in users) {
		try {
			logger.log("Getting script presence for "+user);
			var presence=xmpp.getPresence(user);
			presences.push(presence);
		}
		catch (e) {
			if (users.length==1) {
				throw e;
			}
			logger.warn("Could not get presence of "+user+" due to: "+e);
		}
}

if (presences.length==0) {
	throw "Could not retrieve the presence of any of the specified users";
}

model.presences=presences;