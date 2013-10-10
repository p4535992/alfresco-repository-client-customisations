<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/user-profile/lib/profile.lib.js">

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
