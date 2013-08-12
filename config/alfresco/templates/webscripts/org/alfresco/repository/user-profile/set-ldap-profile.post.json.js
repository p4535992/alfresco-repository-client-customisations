var userName = json.getString("userName");
	
ldap.updateUser(userName, sanitiser.sanitise(json.toString()));

model.result="success";
model.message="";