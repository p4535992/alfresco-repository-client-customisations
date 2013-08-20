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
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/filters.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Main entry point: Create collection of images in the given space (and its subspaces)
 * @method main
 */
function main()
{
   var items = [],
      assets,
      filterParams,
      query;
   
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }
   
   // Use the "tag" filter and an "images" type
   parsedArgs.type = "images";
   filterParams = Filters.getFilterParams("tag", parsedArgs);
   
   // Sort the list before trimming to page chunks 
   assets = search.query(
   {
      query: filterParams.query,
      page:
      {
         maxItems: (filterParams.limitResults ? parseInt(filterParams.limitResults, 10) : 0)
      },
      sort: filterParams.sort
   });
   
   return (
   {
      luceneQuery: filterParams.query,
      items: assets
   });
}

/**
 * Images List Component: images
 */
model.images = main();
