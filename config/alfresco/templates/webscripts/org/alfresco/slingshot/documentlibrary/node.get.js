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
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/evaluator.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/filters.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Main entry point: Return single document or folder given it's nodeRef
 *
 * @method getDoclist
 */
function getDoclist()
{
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }
   
   parsedArgs.pathNode = ParseArgs.resolveNode(parsedArgs.nodeRef);
   parsedArgs.location = Common.getLocation(parsedArgs.pathNode, parsedArgs.libraryRoot);

   var filter = args.filter,
      items = [];

   var favourites = Common.getFavourites(),
      node = parsedArgs.pathNode,
      parent =
      {
         node: node.parent,
         userAccess: Evaluator.run(node.parent, true).actionPermissions
      };

   var isThumbnailNameRegistered = thumbnailService.isThumbnailNameRegistered(THUMBNAIL_NAME),
      thumbnail = null,
      item = Evaluator.run(node);

   item.isFavourite = (favourites[node.nodeRef] === true);

   item.location =
   {
      site: parsedArgs.location.site,
      siteTitle: parsedArgs.location.siteTitle,
      container: parsedArgs.location.container,
      path: parsedArgs.location.path,
      file: node.name
   };

   item.location.parent = {};
   if (node.parent != null && node.parent.hasPermission("Read"))
   {
      item.location.parent.nodeRef = String(node.parent.nodeRef.toString());  
   }

   // Special case for container and libraryRoot nodes
   if ((parsedArgs.location.containerNode && String(parsedArgs.location.containerNode.nodeRef) == String(node.nodeRef)) ||
      (parsedArgs.libraryRoot && String(parsedArgs.libraryRoot.nodeRef) == String(node.nodeRef)))
   {
      item.location.file = "";
   }
      
   // Is our thumbnail type registered?
   if (isThumbnailNameRegistered && item.node.isSubType("cm:content"))
   {
      // Make sure we have a thumbnail.
      thumbnail = item.node.getThumbnail(THUMBNAIL_NAME);
      if (thumbnail === null)
      {
         // No thumbnail, so queue creation
         item.node.createThumbnail(THUMBNAIL_NAME, true);
      }
   }
   
   // Request presence information for the item's creator and modifier
   item.modifiedByUserPresence = presenceService.getUserPresence(item.modifiedBy.userName);
   item.createdByUserPresence = presenceService.getUserPresence(item.createdBy.userName);      
   
   item.archivalStatus = managedDeletion.getArchivalDetails(item.node);
   item.perishReason = managedDeletion.getPerishReason(item.node.properties["md:perishReason"]);
   
   return (
   {
      parent: parent,
      onlineEditing: utils.moduleInstalled("org.alfresco.module.vti"),
      items: [item]
   });
}

/**
 * Document List Component: doclist
 */
model.doclist = getDoclist();
