<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/user-profile/lib/profile.lib.js">

//Before we begin, we need to do a check about who can edit whose profiles
//An Administrator can edit anyone's profile, everyone else can only edit their own
var specifiedUserName = args.user;
var myName = person.properties["cm:userName"];
var content;
var filename;

for each (field in formdata.fields)
{
   logger.log("Found field: "+field.name);
   switch (String(field.name).toLowerCase())
   {
     case "avatar":
   	  	content=field.content;
   	  	filename=field.filename;   	  	
     break;
     
   }
}

if (myName==specifiedUserName || people.isAdmin(person))
{

  //Set the internal copy of the profile.  We don't need to update LDAP as LDAP will simply maintain a static reference to the avtar
  model.nodeRef=setAvatar(specifiedUserName, content, filename);

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