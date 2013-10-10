<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/wiki/lib/wiki.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">

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
 * Update specified wiki page.
 *
 * @method PUT
 * @param uri {string} /slingshot/wiki/page/{siteid}/{pageTitle}
 */
function getTemplateParams()
{
   // Grab the URI parameters
   var siteId = "" + url.templateArgs.siteId;
   var pageTitle = "" + url.templateArgs.pageTitle;

   if (siteId === null || siteId.length === 0)
   {
      return null;
   }

   if (pageTitle === null || pageTitle.length === 0)
   {
      return null;
   }

   return (
   {
      "siteId": siteId,
      "pageTitle": pageTitle
   });
}

function update()
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
      return jsonError("Could not find site: " + siteId);
   }

   var wiki = getWikiContainer(site);
   if (wiki === null)
   {
      return jsonError("Could not locate wiki container");
   }
   
   var page = wiki.childByNamePath(params.pageTitle), activityType;
   
   if (page==null)
   {
	  logger.log("New wiki page being created - invalidating name cache");
	  wikiPageNames.invalidate(wiki.qnamePath);
   }
   
   //Get the enhanced security label properties (If they exist)
   var   nod = null,
         pm=null,
         freeformcaveats=null,
         closedmarkings = null,
         openmarkings = null,
         organisations = null,
         eyes = null,
         atomal = null
         ;
         
   //If we've got a pre-existing version of the page, but don't have the esl properties
   //set in the JSON request, we're almost certainley doing a revert, so copy the existing
   //security label in.
   if (page!=null && !json.has("eslProtectiveMarking"))
   {
     nod=page.properties["es:nod"];
     pm=page.properties["es:pm"];
     freeformcaveats=page.properties["es:freeFormCaveats"];
     eyes=page.properties["es:nationalityCaveats"];
     closedMarkingsArr=page.properties["es:closedMarkings"];
     if (closedMarkingsArr!=null)
     {
       closedmarkings=closedMarkingsArr.join(",");
     }
     openMarkingsArr=page.properties["es:openMarkings"];
     if (openMarkingsArr!=null)
     {
       openmarkings=openMarkingsArr.join(",");
     }
   }
   else //we are doing a normal page update
   {
   
     nod=json.get("eslNationalOwner");
     pm=json.get("eslProtectiveMarking");
     freeformcaveats=json.get("eslCaveats");
   
     //Note that this is less logic than is in the client-side javascript
     //We're not trying to replicate the formatting here, just protect against XSS
     //and the formatting rules happen to make it easy
     freeformcaveats=freeformcaveats.replaceAll('[^a-zA-Z ]','');
   
     closedmarkings=json.get("eslClosedGroupsHidden");
     atomal = json.get("eslAtomal");
     if (closedmarkings!=null)
     {
         closedmarkings=closedmarkings+atomalToGroups(atomal, closedmarkings.length()>0);
     }
     else
     {
         closedmarkings=closedmarkings=atomalToGroups(atomal, false);
     }
     organisations=json.get("eslOrganisationsHidden");
     eyes=json.get("eslNationalCaveats");
   }
   
   // Create the page if it doesn't exist
   if (page === null)
   {
      page = createWikiPage(params.pageTitle, wiki,
      {
         content: json.get("pagecontent"),
         versionable: true
      });
   
      activityType = "org.alfresco.wiki.page-created";
   }
   else 
   {
      // Check the version of the page being submitted against the version now in the repo, or a forced save
      if (pageVersionMatchesSubmitted(page) || json.has("forceSave"))
      {
         // Create a new revision of the page
         var workingCopy = page.checkout();
         workingCopy.content = json.get("pagecontent");
         workingCopy.checkin();
         page.save();

         activityType = "org.alfresco.wiki.page-edited";
      }
      else
      {
         status.setCode(status.STATUS_CONFLICT, "Repository version is newer.");
         return;
      }
   }
   
   var data =
   {
      title: params.pageTitle.replace(/_/g, " "),
      page: json.get("page") + "?title=" + params.pageTitle
   }
   // Log activity   
   activities.postActivity(activityType, params.siteId, "wiki", jsonUtils.toJSONString(data));
   
   if (!json.isNull("tags"))
   {
      var tags = Array(json.get("tags"));
      if (tags) 
      {
         // This is so unnecessary!
         // A much cleaner approach would be to just pass in the tags as a space separated
         // string and call the (native) method split
         var tags = [];
         var tmp = json.get("tags");
         for (var x = 0, xx = tmp.length(); x < xx; x++)
         {
            tags.push(tmp.get(x));
         }
         page.tags = tags;
      }
      else
      {
         page.tags = []; // reset
      }
      

	//Set ESC properties
	//Iff we've got a PM (mandatory property of an ESL) then populate the whole ESL
	         if (pm != null && pm != "") {
	        	 page.properties["es:nod"] = nod;
	        	 page.properties["es:pm"] = pm;
	        	 page.properties["es:freeFormCaveats"] = freeformcaveats;
	        	 page.properties["es:nationalityCaveats"] = eyes;
	        	 if (closedmarkings != null && closedmarkings != "") {
	        		 page.properties["es:closedMarkings"] = closedmarkings.split(",");
	        	 }
	        	 else {
	        	 	page.properties["es:closedMarkings"] = null;
	        	 }
	        	 if (organisations != null && organisations != "") {
	        		 page.properties["es:organisations"] = organisations.split(",");
	        	 }
	        	 else {
	        	 	page.properties["es:organisations"] = null;
	        	 }
	        	 //Only set the open marking if reverting a page - otherwise, leave them alone
	        	 if (page!=null && !json.has("eslProtectiveMarking"))
	             {
		        	 if (openmarkings != null && openmarkings != "") {	        		 
		        		 page.properties["es:openMarkings"] = openmarkings.split(",");
		        	 }
		        	 else {
		        	 	 page.properties["es:openMarkings"] = null;
		        	 }
	             }
         }


      page.save();
   }
   
   // NOTE: for now we return the raw page content and do the transformation
   // of any wiki markup on the client. This is because the edit view needs to display
   // the raw content (for editing) whereas the page view needs to display the rendered content.
   return (
   {
      page: page,
      tags: page.tags
   });
}

/**
 * Checks whether the current repository version is newer than a submitted version number.
 * Returns:
 *    false if currentVersion is older than repoVersion
 *    true  otherwise
 */
function pageVersionMatchesSubmitted(page)
{
   var currentVersion = "0",
      repoVersion = "0";
   
   if (json.has("currentVersion"))
   {
      currentVersion = json.get("currentVersion");
   }
   
   if (page.hasAspect("cm:versionable"))
   {
      repoVersion = getLatestVersion(page.versionHistory);
   }
   else
   {
      page.addAspect("cm:versionable");
      page.save();
      return 0;
   }
   
   return (sortByLabel(
   {
      label: repoVersion
   },
   {
      label: currentVersion
   }) != -1);
}

function sortByLabel(version1, version2)
{
   if ((version1.label.indexOf(".") == -1) || (version2.label.indexOf(".") == -1))
   {
      return -1;
   }
   
   var major1 = new Number(version1.label.substring(0, version1.label.indexOf(".")));
   var major2 = new Number(version2.label.substring(0, version2.label.indexOf(".")));
   if (major1 - 0 == major2 - 0)
   {
        var minor1 = new Number(version1.label.substring(version1.label.indexOf(".")+1));
        var minor2 = new Number(version2.label.substring(version2.label.indexOf(".")+1));
        return (minor1 < minor2) ? 1 : (minor1 > minor2) ? -1 : 0;
   }
   else
   {
       return (major1 < major2) ? 1 : -1;
   }
}

function getLatestVersion(versionHistory)
{
   versionHistory.sort(sortByLabel);
   return versionHistory[0].label;
}

model.result = update();
