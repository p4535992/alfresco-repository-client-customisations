<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/user-profile/lib/profile.lib.js">

//Before we begin, we need to do a check about who can edit whose profiles
//An Administrator can edit anyone's profile, everyone else can only edit their own
var specifiedUserName = json.getString("userName");
var myName = person.properties["cm:userName"];

if (myName==specifiedUserName || people.isAdmin(person))
{


  //Set the external LDAP profile (we don't have proper transactions so do this first as it's more likely
  //to go wrong)
  ldap.updateUser(specifiedUserName, json.toString());

  //Next, set the internal copy of the profile
  var postData = parseJsonData();
  setProfileData(postData);

  //Lastly, record the success
  model.result="success";
  model.message="";
}
else
{
  model.result="failure";
  model.message="Permission denied.  "+myName+" does not have permission to edit the profile of "+specifiedUserName
  	+". Only "+specifiedUserName+" or an Administrator may do that.";
}