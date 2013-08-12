var user = person.properties.userName;

var headers = xmpp.getUnreadMessageHeaders(user);

var scriptMessageHeaders=[],
	scriptInviteHeaders=[],
	messageCount = 0;


logger.log(headers);
for (var i=0; i < headers.size(); i++) {
logger.log('Found a header: '+headers.get(i));

        var header=headers.get(i);
        var jid = header.getJid();
        
        // Detect whether message is group invite
        if((jid.contains(".hablar-emite-")) && (jid.contains(".securitylabel-"))) {
        	scriptInviteHeaders[jid]=jid;
        	continue;
        }
        else {
        	
            var thisUser = header.getJid().substring(0, header.getJid().indexOf("@")); //TODO: remove everything after the @
            logger.log('user '+thisUser);
            if (scriptMessageHeaders[thisUser]) {
                    scriptMessageHeaders[thisUser]++;
            }
            else {
                    scriptMessageHeaders[thisUser]=1;
            }
        	
        }
        
}

model.messages=[];
model.invites=[];

for (var user in scriptMessageHeaders) {
	var message={};
	var person = people.getPerson(user);
	message.userName=user;
	if (person!=null && person.properties!=null) {
	  message.displayName=(person.properties.firstName + " " + person.properties.lastName).replace(/^\s+|\s+$/g, "");
	} else {
	  message.displayName=message.userName;
	}
	message.count=scriptMessageHeaders[user];
	model.messages.push(message);
	messageCount+=message.count;
}

for (var inviteJid in scriptInviteHeaders) {
	var invite = {};
	// format group chat name
    var groupChatName = inviteJid.substring(0, inviteJid.indexOf("."));
    groupChatName = groupChatName.replace(/_/g," ");
    groupChatName = groupChatName.charAt(0).toUpperCase() + groupChatName.slice(1);
	invite.groupChatName = groupChatName;
	model.invites.push(invite);
}

model.count = messageCount + model.invites.length;