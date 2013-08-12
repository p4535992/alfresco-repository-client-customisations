<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/user-profile/lib/profile.lib.js">

var postData = parseJsonData();
setProfileData(postData);

model.result="success";
model.message="";