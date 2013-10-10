<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/action/action.lib.js">

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

/**
 * Safe move multiple files action
 * @method POST
 */

/**
 * Entrypoint required by action.lib.js
 *
 * @method runAction
 * @param p_params {object} Object literal containing files array
 * @return {object|null} object representation of action results
 */
function runAction(p_params)
{
   var results = [],
      destNode = p_params.destNode,
      files = p_params.files,
      file, fileNode, result, nodeRef;

   // Must have array of files
   if (!files || files.length === 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "No files.");
      return;
   }

   for (file in files)
   {
      nodeRef = files[file];
      result =
      {
         action: "safeMoveFile",
         success: false
      };
      
      try {
    	  
    	fileNode = search.findNode(nodeRef);
    	
      	result.id = fileNode.name;
      	result.type = (fileNode.getTypeShort() == "cm:folder" ? "folder" : "file");
      	
      	var action = actions.create("com.surevine.alfresco.repo.action.MoveAsSuperuserAction");
      	action.parameters.destination = destNode.getNodeRef();
      	action.execute(search.findNode(nodeRef));
      	
      	result.previousNodeRef = nodeRef,
        result.nodeRef = action.parameters.result,
      	result.success = true;
      	
      }
      catch (e)
      {
          result.id = file;
          result.nodeRef = nodeRef;
          result.success = false;
      }

      results.push(result);
   }

   return results;
}

/* Bootstrap action script */
main();
