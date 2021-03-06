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
var PeopleCache = {};

/**
 * Gets / caches a person object
 * @method getPerson
 * @param username {string} User name
 */
function getPerson(username)
{
   if (typeof PeopleCache[username] == "undefined")
   {
      var person = people.getPerson(username);
      if (person == null)
      {
         if (username == "System" || username.match("^System@") == "System@")
         {
            // special case for the System users
            person =
            {
               properties:
               {
                  userName: "System",
                  firstName: "System",
                  lastName: "User"
               },
               assocs: {}
            };
         }
         else
         {
            // missing person - may have been deleted from the database
            person =
            {
               properties:
               {
                  userName: username,
                  firstName: "",
                  lastName: ""
               },
               assocs: {}
            };
         }
      }
      PeopleCache[username] =
      {
         userName: person.properties.userName,
         firstName: person.properties.firstName,
         lastName: person.properties.lastName,
         displayName: (person.properties.firstName + " " + person.properties.lastName).replace(/^\s+|\s+$/g, "")
      };
   }
   return PeopleCache[username];
}

function main()
{
   var json = "",
      versions = [];

   // allow for content to be loaded from id
   if (args["nodeRef"] != null)
   {
      var nodeRef = args["nodeRef"],
         node = search.findNode(nodeRef),
         versionHistory, version, p;

      if (node != null)
      {
         var versionHistory = node.versionHistory;
         if (versionHistory != null)
         {
            for (i = 0; i < versionHistory.length; i++)
            {
               version = versionHistory[i];
               p = getPerson(version.creator);
               versions[versions.length] =
               {
                  nodeRef: version.node.nodeRef.toString(),
                  name: version.node.name,
                  label: version.label,
                  description: version.description,
                  createdDate: version.createdDate,
                  creator:
                  {
                     userName: p.userName,
                     firstName: p.firstName,
                     lastName: p.lastName
                  },
                  creatorPresence: presenceService.getUserPresence(p.userName)                	  
               };
            }
         }
         else
         {
            p = getPerson(node.properties.creator);
            versions[0] =
            {
               nodeRef: node.nodeRef.toString(),
               name: node.name,
               label: "1.0",
               description: "",
               createdDate: node.properties.created,
               creator:
               {
                  userName: p.userName,
                  firstName: p.firstName,
                  lastName: p.lastName
               },
               creatorPresence: presenceService.getUserPresence(p.userName)
            };
         }
      }
   }

   // store node onto model
   model.versions = versions;
}

main();
