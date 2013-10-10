<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/wiki/lib/wiki.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

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

var siteId = url.templateArgs.siteId;
model.siteId = siteId;

var pageMetaOnly = (args.pageMetaOnly == "true");
model.pageMetaOnly = pageMetaOnly;

var filter = args.filter;
model.wiki = getWikiPages(siteId, filter);

function getWikiPages(siteId)
{
   if (siteId === null || siteId.length === 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Site not found: '" + siteId + "'");
      return;
   }
   
   var site = siteService.getSite(siteId);
   if (site === null)
   {
      status.setCode(status.STATUS_NOT_FOUND, "Site not found: '" + siteId + "'");
      return;
   }
   
   var wiki = getWikiContainer(site);
   if (wiki === null)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Wiki container not found");
      return;
   }
   
   var query = "+PATH:\"" + wiki.qnamePath + "//*\" ";
   query += " +(@\\{http\\://www.alfresco.org/model/content/1.0\\}content.mimetype:application/octet-stream OR";
   query += "  @\\{http\\://www.alfresco.org/model/content/1.0\\}content.mimetype:text/html)";
   query += " -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"";
   query += " -TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\"";
   
   if (filter)
   {
      query += getFilterQuery(filter);
   }
   
   var wikiPages = search.luceneSearch(query);
   
   if (pageMetaOnly)
   {
      return ( 
      {
         "container": wiki,
         "pages": wikiPages
      });
   }
   else
   {
   var pages = [];   
   var page, createdBy, modifiedBy, createdByUserPresence, modifiedByUserPresence;
   
   var deletionAuthorisation="normal";
   if (deletionState!="notDeleted" && isDeleter()) //As an optimisation, we leave deletionAuthorisation set to normal if the item isn't deleted as deleters and non-deleters have the same privs here
   {
      deletionAuthorisation="deleter";
   }
   
   var isDeletable = 1;

   if( siteId.length >= 12) {
 	  //could be a deletedItems site...
 	  if( siteId.substr( siteId.length - 12) === "deletedItems" ) {
 		  //it is a deletedItems site, thus, items are not deletable:
 		  isDeletable = 0;
 	  }
   }
   
   for each (page in wikiPages)
   {
      createdBy = people.getPerson(page.properties["cm:creator"]);
      modifiedBy = people.getPerson(page.properties["cm:modifier"]);
      
      createdByUserPresence = presenceService.getUserPresence(page.properties["cm:creator"]);
      modifiedByUserPresence = presenceService.getUserPresence(page.properties["cm:modifier"]);
      
      //ESL values
      var eslPM = "",
          eslNod = "",
          eslFreeformCaveats="",
          eslEyes="",
          eslOpenMarkings="",
          eslOrganisations="",
          eslClosedMarkings="",
          eslAtomal="";
      
      eslPM=page.properties["es:pm"];
      eslNod=page.properties["es:nod"];
      eslFreeformCaveats=page.properties["es:freeFormCaveats"];
      
      if (eslFreeformCaveats==null)
      {
         eslFreeformCaveats="";
      }
      
      eslEyes=page.properties["es:nationalityCaveats"];
      eslClosedMarkings = new Array();
      eslClosedMarkings = page.properties["es:closedMarkings"];
      var seperatedClosed = seperateAtomalFromClosedMarkings(eslClosedMarkings);
      eslClosedMarkings = seperatedClosed.closedMarkings;
      eslAtomal = seperatedClosed.atomal;
      eslClosedMarkings = seperatedClosed.closedMarkings;
      eslOpenMarkings = new Array();
      eslOpenMarkings = page.properties["es:openMarkings"];
      eslOrganisations = new Array();
      eslOrganisations = page.properties["es:organisations"];
      
      var deletionState="notDeleted";

      if (page.hasAspect("md:deleted"))
      {
         deletionState="deleted";
      }
      else if (page.hasAspect("md:markedForDeletion"))
      {
   	   deletionState="markedForDeletion";
      }

      
      pages.push(
      {
         "page": page,
         "tags": page.tags,
         "modified": page.properties.modified,
         "createdBy": createdBy,
         "modifiedBy": modifiedBy,
         "createdByUserPresence": createdByUserPresence,
         "modifiedByUserPresence": modifiedByUserPresence,
         "eslPM": eslPM,
	     "eslNod": eslNod,
	     "eslEyes": eslEyes,
	     "eslFreeformCaveats": eslFreeformCaveats,
	     "eslOpenMarkings": eslOpenMarkings,
         "eslClosedMarkings": eslClosedMarkings,
         "eslOrganisations": eslOrganisations,
         "eslAtomal": eslAtomal,
         "deletionState": deletionState
      });
   }
   
   return ( 
   {
	  "isDeletable":isDeletable,	//should use JavaScript boolean here, but the ftl throws an error when booleans are used!
      "container": wiki,
      "pages": pages,
      "deletionAuthorisation": deletionAuthorisation
   });
   }
}

function getFilterQuery(filter)
{
   var filterQuery = "";
   
   switch (String(filter))
   {
      case "all":
         // Nothing to do
         break;
      
      case "recentlyModified":
         var usingModified = true;
         // fall through...
      case "recentlyAdded":
         // Which query: created, or modified?
         var dateField = "modified";
         if (typeof usingModified === "undefined")
         {
            dateField = "created";
         }

         // Default to 7 days - can be overridden using "days" argument
         var dayCount = 7;
         var argDays = args["days"];
         if ((argDays != null) && !isNaN(argDays))
         {
            dayCount = argDays;
         }
         var date = new Date();
         var toQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();
         date.setDate(date.getDate() - dayCount);
         var fromQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();

         filterQuery += "+@cm\\:" + dateField + ":[" + fromQuery + "T00\\:00\\:00 TO " + toQuery + "T23\\:59\\:59] ";
         break;
         
      case "myPages":
         filterQuery += "+@cm\\:creator:\"" + person.properties.userName + '"';
         break;   
   }
   
   return filterQuery;
}
