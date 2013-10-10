<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/wiki/lib/wiki.lib.js">

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
 * Renames a wiki page. Updates the name of the current page
 * and creates a link with the previous name that points to the page;
 * this is done so that all references to the old page will still work.
 *
 * @method POST
 * @param uri {string} /slingshot/wiki/page/{siteid}/{pageTitle}
 */
model.result = main();

function main()
{
   if (json.isNull("name"))
   {
      return jsonError("No new name property specified");
   }
   // Remove any whitespace and replace with "_"
   var newName = new String(json.get("name"));
   newName = newName.replace(/\s+/g, "_");
   
   var params = getTemplateArgs(["siteId", "pageTitle"]);
   
   // Get the site
    var site = siteService.getSite(params.siteId);
    if (site === null)
    {
      return jsonError("Could not find site: " + params.siteId);
    }

   var wiki = getWikiContainer(site);
   if (wiki === null)
   {
      return jsonError("Could not locate wiki");
   }

   var page = wiki.childByNamePath(params.pageTitle);
   if (!page)
   {
      return jsonError("Could not find specified page.");
   }
   
   var existing = wiki.childByNamePath(newName);
   {
      if (existing)
      {
         status.setCode(status.STATUS_CONFLICT, "Duplicate name.");
         return;
      }
   }
   
   // Finally, now we can do what we are supposed to do
   var currentName = new String(page.name);
   
   page.name = newName;
   page.properties["cm:title"] = new String(newName).replace(/_/g, " ");
   page.save();
   
   var placeholder = createWikiPage(currentName, wiki,
   {
      content: msg.get("page-moved") + " [[" + newName + "|" + msg.get("page-moved-here") + "]]."
   });
      
   //Set the ESL attributes of the placeholder == those of the "new" page
   
   if (page.hasAspect("es:enhancedSecurityLabel"))
   {
   
	   var eslProps = new Array(6);
	   
	   //First, get the ESL attributes of the renamed page
	   var nod=page.properties["es:nod"];
	   var pm=page.properties["es:pm"];
	   var freeformcaveats=page.properties["es:freeFormCaveats"];
	   var eyes=page.properties["es:nationalityCaveats"];
	   var closedMarkingsArr=page.properties["es:closedMarkings"];
	   var openMarkingsArr=page.properties["es:openMarkings"];
       var organisationsArr=page.properties["es:organisations"];
	   
	   //Next, set the attributes of the placeholder page
	   eslProps["es:nod"]=nod;
	   eslProps["es:pm"]=pm;
	   eslProps["es:freeFormCaveats"]=freeformcaveats;
	   eslProps["es:nationalityCaveats"]=eyes;
	   eslProps["es:closedMarkings"]=closedMarkingsArr;
	   eslProps["es:openMarkings"]=openMarkingsArr;
	   eslProps["es:organisations"]=organisationsArr;
	   
	   //Now apply the ESL aspect with those properties
	   placeholder.addAspect("es:enhancedSecurityLabel", eslProps);
   }
	   
   //Was the original page marked for deletion?
   if (page.hasAspect("md:markedForDeletion"))
   {
	  //If so, copy the managed deletion properties of the original page into the placeholder
	  var props = new Array(2);
	  props["md:archivalDue"]=page.properties["md:archivalDue"];
	  props["md:deletedBy"]=page.properties["md:deletedBy"];
	  placeholder.addAspect("md:markedForDeletion", props);
   }
   
   placeholder.save();
      
   var data =
   {
      title: newName.replace(/_/g, " "),
      page: json.get("page") + "?title=" + newName,
      custom0: currentName.replace(/_/g, " ")
   }

   activities.postActivity("org.alfresco.wiki.page-renamed", params.siteId, "wiki", jsonUtils.toJSONString(data));
   
   return (
   {
      name: newName // Return the new name to the client
   });
}
