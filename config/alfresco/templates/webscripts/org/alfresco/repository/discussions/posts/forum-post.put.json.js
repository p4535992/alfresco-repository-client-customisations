<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/requestutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/discussions/topicpost.lib.js">
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

const ASPECT_SYNDICATION = "cm:syndication";
const PROP_PUBLISHED = "cm:published";
const PROP_UPDATED = "cm:updated";

/**
 * Updates the passed forum post node.
 * @param topic the topic node if the post is the top level post
 * @param post the post node.
 */
function updatePost(topic, post)
{
   var title = "";
   if (json.has("title"))
   {
      title = json.get("title");
   }
   var content = "";
   if (json.has("content"))
   {
      content = json.get("content");
   }
   
   // get the tags JSONArray and copy it into a real javascript array object
   var tags = [];
   if (json.has("tags"))
   {
	   tmp = json.get("tags");
	      for (var x=0; x < tmp.length(); x++)
	      {
	          tags.push(tmp.get(x));
	      }
   }
   
   //Get the enhanced security label properties (If they exist)
   var	nod = null,
   		pm=null,
        freeformcaveats=null,
        closedmarkings = null,
        openmarkings = null,
        organisations = null,
        eyes = null,
        atomal = null;

   nod=json.get("eslNationalOwner");
   pm=json.get("eslProtectiveMarking");
   freeformcaveats=json.get("eslCaveats");
   closedmarkings=json.get("eslClosedGroupsHidden");
   openmarkings=json.get("eslOpenGroupsHidden");
   organisations=json.get("eslOrganisationsHidden");
   eyes=json.get("eslNationalCaveats");
   atomal=json.get("eslAtomal");
	
   //Note that this is less logic than is in the client-side javascript
   //We're not trying to replicate the formatting here, just protect against XSS
   //and the formatting rules happen to make it easy
   freeformcaveats=freeformcaveats.replaceAll(/[^a-zA-Z ]+/g,'');   
   
   // update the topic title
   post.properties["cm:title"] = title;

   // make sure the syndication aspect has been added
   if (! post.hasAspect(ASPECT_SYNDICATION))
   {
      var params = [];
      params[PROP_PUBLISHED] = new Date();
      params[PROP_UPDATED] = params[PROP_PUBLISHED];
      post.addAspect(ASPECT_SYNDICATION, params);
   }
   else
   {
      post.properties[PROP_UPDATED] = new Date();
   }
   post.mimetype = "text/html";
   post.content = content;


   //Set ESC properties
   if(closedmarkings != null) {
       closedmarkings=closedmarkings+atomalToGroups(atomal, closedmarkings.length() >0);
   }
   // If we've got a PM (mandatory property of an ESL) then populate the whole ESL
   // both for the content node and the topic node
   if (pm != null && pm != "") {
       post.properties["es:nod"] = nod;
       post.properties["es:pm"] = pm;
       post.properties["es:freeFormCaveats"] = freeformcaveats;
       post.properties["es:nationalityCaveats"] = eyes;
       if (closedmarkings != null && closedmarkings != "") {
           post.properties["es:closedMarkings"] = closedmarkings.split(",");
       }
       else {
           post.properties["es:closedMarkings"] = null;
       }
       if (organisations != null && organisations != "") {
           post.properties["es:organisations"] = organisations.split(",");
       }
       else {
           post.properties["es:organisations"] = null;
       }
   }

   post.save();
   
   // Only set the tags if it is a topic post
   // as we currently don't support individual post tagging
   if (topic != null)
   {
      topic.tags = tags;

      if (pm != null && pm != "") {
          topic.properties["es:nod"] = nod;
          topic.properties["es:pm"] = pm;
          topic.properties["es:freeFormCaveats"] = freeformcaveats;
          topic.properties["es:nationalityCaveats"] = eyes;
          if (closedmarkings != null && closedmarkings != "") {
              topic.properties["es:closedMarkings"] = closedmarkings.split(",");
          }
          else {
              topic.properties["es:closedMarkings"] = null;
          }
          if (openmarkings != null && openmarkings != "") {
              topic.properties["es:openMarkings"] = openmarkings.split(",");
          }
          else {
              topic.properties["es:openMarkings"] = null;
          }
          if (organisations != null && organisations != "") {
              topic.properties["es:organisations"] = organisations.split(",");
          }
          else {
              topic.properties["es:organisations"] = null;
          }
      }
      
      topic.save();
/*      
      // Set the node as perishable if required
      if(json.has("perishable")) {
   	    var perishableReason = json.get("perishable");
        managedDeletion.setPerishable(topic, perishableReason ? perishableReason : null);
      }
*/
   }
}

function main()
{
   // get requested node
   var node = getRequestNode();
   if (status.getCode() != status.STATUS_OK)
   {
      return;
   }
   
   // find the post node if this is a topic node
   var topicNode = null;
   var postNode = null;
   if (node.type == "{http://www.alfresco.org/model/forum/1.0}post")
   {
      postNode = node;
   }
   else if (node.type == "{http://www.alfresco.org/model/forum/1.0}topic")
   {
      topicNode = node;
      var nodes = getOrderedPosts(node);
      if (nodes.length > 0)
      {
         postNode = nodes[0];
      }
      else
      {
         status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, "First post of topic node" + node.nodeRef + " missing");
         return;
      }
   }
   else
   {
      status.setCode(STATUS_BAD_REQUEST, "Incompatible node type. Required either fm:topic or fm:post. Received: " + node.type);
      return;
   }
   
   // update
   updatePost(topicNode, postNode);

   // Due to https://issues.alfresco.com/browse/ALFCOM-1775
   // we have to reuse the search results from before altering the nodes,
   // that's why we don't use the function fetchPostData here (which would
   // do another lucene search in case of a topic post
   if (topicNode == null)
   {
      model.postData = getReplyPostData(postNode);
      
      // add an activity item
      if (json.has("site") && json.has("page"))
      {
         var topicData = getTopicPostData(model.postData.post.parent);
         var data =
         {
            title: topicData.post.properties.title,
            page: json.get("page") + "?topicId=" + topicData.topic.name,
            params:
            {
               topicId: topicData.topic.name
            }
         }
         activities.postActivity("org.alfresco.discussions.reply-updated", json.get("site"), "discussions", jsonUtils.toJSONString(data));
      }
   }
   else
   {
      // we will do the search here as we have to reuse the lucene result later
      // See above, use getTopicPostDataFromTopicAndPosts instead of getTopicPostData
      //model.topicpost = getTopicPostData(node);
      model.postData = getTopicPostDataFromTopicAndPosts(topicNode, nodes);
      
      // add an activity item
      if (json.has("site") && json.has("page"))
      {
         var topicData = getTopicPostData(model.postData.post.parent);
         var data =
         {
            title: topicData.post.properties.title,
            page: json.get("page") + "?topicId=" + topicData.topic.name
         }
         activities.postActivity("org.alfresco.discussions.post-updated", json.get("site"), "discussions", jsonUtils.toJSONString(data));
      }
   }
}
main();
