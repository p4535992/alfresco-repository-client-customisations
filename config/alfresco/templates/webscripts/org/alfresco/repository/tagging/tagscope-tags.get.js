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
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/requestutils.lib.js">

function findTargetNode()
{
   if (url.templateArgs.site != undefined)
   {
      var siteId = url.templateArgs.site;
      var containerId = url.templateArgs.container;
      var path = url.templateArgs.path;

      // fetch site
      var site = siteService.getSite(siteId);
      if (site === null)
      {
         status.setCode(status.STATUS_NOT_FOUND, "Site " + siteId + " does not exist");
         return null;
      }
      else if (containerId == undefined)
      {
         // get site node
         return site.node;
      }
      
      // fetch container
      var node = null;
      if (site.hasContainer(containerId))
      {
         node = site.getContainer(containerId);
      }
      if (node == null)
      {
         // Container might not be there as it hasn't been created yet!
         //status.setCode(status.STATUS_NOT_FOUND, "Unable to fetch container '" + containerId + "' of site '" + siteId + "'. (No write permission?)");
         return null;
      }
      else if (path == undefined)
      {
         return node;
      }
      
      node = node.childByNamePath(path);
      return node;
   }
   else
   {
      return findFromReference();
   }
}

function main()
{
   var node = findTargetNode();
   if (node == null)
   {
      model.noscopefound=true;
      return;
   }
   
   // fetch the nearest available tagscope
   var scope = node.tagScope;
   if (scope == null)
   {
      //status.setCode(status.STATUS_BAD_REQUEST, "No tag scope could be found for the given resource");
      //return null;
      model.noscopefound=true;
   }
   else
   {
      var topN = args["topN"] != undefined ? parseInt(args["topN"]) : 30;
      if (topN == -1)
      {
    	  topN=30;
      }
      if (topN > -1)
      {
         // PENDING:
         // getTopTags currently throws an AIOOB exception if topN > tags.length() :-/
         if (scope.tags.length < topN) topN = scope.tags.length;
         model.tags = scope.getTopTags(topN);
      }
      else
      {
         model.tags = scope.tags;
      }
   }
}

main();
