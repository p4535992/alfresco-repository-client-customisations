var user = person.properties.userName;

var presence = args['presence'];

var status = args['status'];
if (status===null) {
	status='';
}

model.result = xmpp.setPresence(user, presence, status);

model.presence = '{ "user": "'+user+'", "mode": "'+presence+'", "status": "'+status+'", "source": "space" }';