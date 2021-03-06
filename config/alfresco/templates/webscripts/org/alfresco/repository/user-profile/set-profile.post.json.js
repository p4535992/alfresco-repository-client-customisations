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
