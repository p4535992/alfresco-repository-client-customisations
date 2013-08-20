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
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/wiki/lib/wiki.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

/**
 * Get wiki page properties.
 * Returns an error message if the specified page cannot be found.
 *
 * @method GET
 * @param uri {string} /slingshot/wiki/page/{siteid}/{pageTitle}
 */
function main()
{

   var params = getTemplateArgs(["siteId", "pageTitle"]);
   if (params === null)
   {
      return jsonError("No parameters supplied");
   }

   // Get the site
   var site = siteService.getSite(params.siteId);
   if (site === null)
   {
      // Wiki "not found" error is used elsewhere
      return status.setCode(status.STATUS_PRECONDITION_FAILED, "Could not find site: " + params.siteId);
   }

   var wiki = getWikiContainer(site);
   if (wiki === null)
   {
      return jsonError("Could not locate wiki");
   }
 
   var page = wiki.childByNamePath(params.pageTitle);
   if (!page)
   {
      model.container = wiki;
      return status.setCode(status.STATUS_NOT_FOUND, "The page \"" + params.pageTitle.replace(/_/g, " ") + "\" does not exist.");
   }

   // Figure out what (internal) pages this page contains links to
   var content = page.content.toString();
   var re = /\[\[([^\|\]]+)/g;
    
   var links = [], result, match, matched_p, matchedSoFar = [], j;
   while ((result = re.exec(content)) !== null)
   {
      match = result[1];
      matched_p = false;
      // Check for duplicate links
      for (j = 0; j < matchedSoFar.length; j++)
      {
         if (match === matchedSoFar[j])
         {
            matched_p = true;
            break;
         }
      }
      
      if (!matched_p)
      {
         matchedSoFar.push(match);
         links.push(match);
      }
   }

   // Also return complete list of pages to resolve links
   var wikiPages = wikiPageNames.getWikiPageNames(wiki.qnamePath);
   var pageList = [];
   
   for (var i=0; i<wikiPages.size(); i++) {
	    var p = wikiPages.get(i);
	    pageList.push(p);
   }
   
   // Enhanced security check properties
   var eslNS="{http://www.alfresco.org/model/enhancedSecurity/0.3}";
   var theESLNod=page.properties[eslNS + "nod"];
   var theESLPM=page.properties[eslNS + "pm"];
   var theESLFreeFormCaveats=page.properties[eslNS + "freeFormCaveats"];
   var theESLClosed = new Array();
   theESLClosed = page.properties[eslNS + "closedMarkings"];
   var seperatedClosed = seperateAtomalFromClosedMarkings(theESLClosed);
   theESLClosed = seperatedClosed.closedMarkings;
   var theESLAtomal = seperatedClosed.atomal;
   
  
   var theESLOpen = new Array();
   theESLOpen = page.properties[eslNS + "openMarkings"];
   var theESLOrganisation = new Array();
   theESLOrganisation = page.properties[eslNS+"organisations"];
   
   var theESLEyes = page.properties[eslNS + "nationalityCaveats"];
   
   if (theESLNod==null) {
      theESLNod="";
   }
   if (theESLPM==null) {
      theESLPM="";
   }
   if (theESLFreeFormCaveats==null) {
      theESLFreeFormCaveats="";
   }
   if (theESLEyes==null) {
      theESLEyes="";
   }
   
   var deletionState="notDeleted";
   if (page.hasAspect("md:deleted"))
   {
      deletionState="deleted";
   }
   else if (page.hasAspect("md:markedForDeletion"))
   {
	   deletionState="markedForDeletion";
   }

   var deletionAuthorisation="normal";
   if (deletionState!="notDeleted" && isDeleter()) //As an optimisation, we leave deletionAuthorisation set to normal if the item isn't deleted as deleters and non-deleters have the same privs here
   {
      deletionAuthorisation="deleter";
   }

   var isDeletable = "yes";
   if( params.siteId.length >= 12) {
 	  //could be a deletedItems site... (site name needs to be at least 12 characters to contain the "deletedItems" suffix
 	  if( params.siteId.substr( params.siteId.length - 12) == "deletedItems" ) {
 		  //it is a deletedItems site, thus, items are not deletable:
 		  isDeletable = "no";
 	  }
   }
   
   if (params.pageTitle == "Main_Page")
   {
	   isDeletable = "no";
   }
   
   return (
   {
	  isDeletable: isDeletable,	//should use JavaScript boolean here, but the ftl throws an error when booleans are used!
      page: page,
      container: wiki,
      tags: page.tags,
      links: links,
      pageList: pageList,
      eslNod: theESLNod,
      eslPM: theESLPM,
      eslFreeFormCaveats: theESLFreeFormCaveats,
      eslOpen: theESLOpen,
      eslClosed: theESLClosed,
      eslEyes: theESLEyes,
      eslAtomal: theESLAtomal,
      eslOrganisations: theESLOrganisation,
      deletionState: deletionState,
      deletionAuthorisation: deletionAuthorisation
   });
}

model.result = main();
