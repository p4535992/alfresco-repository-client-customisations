<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/requestutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/nodenameutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/discussions/topicpost.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">

function ensureTagScope(node)
{
   if (! node.isTagScope)
   {
      node.isTagScope = true;
   }
   
   // also check the parent (the site!)
   if (! node.parent.isTagScope)
   {
      node.parent.isTagScope = true;
   }
}

/**
 * Adds a post to the passed forum node.
 */
function createPost(forumNode)
{
   // fetch the data required to create a topic
   var title = "";
   if (json.has("title"))
   {
      title = json.get("title");
   }
   var content = "";
   if (json.has("title"))
   {
      content = json.get("content");
   }
   var tags = [];
   if (json.has("tags"))
   {
      // get the tags JSONArray and copy it into a real javascript array object
      var tmp = json.get("tags");
      for (var x=0; x < tmp.length(); x++)
      {
          tags.push(tmp.get(x));
      }
   }
   
      //Get the enhanced security label properties (If they exist)
      var   nod = null,
            pm=null,
            freeformcaveats=null,
            closedmarkings = null,
            organisations = null,
            eyes = null,
            atomal = null
            ;
      
      nod=json.get("eslNationalOwner");
      pm=json.get("eslProtectiveMarking");
      freeformcaveats=json.get("eslCaveats");
      closedmarkings=json.get("eslClosedGroupsHidden");
      organisations=json.get("eslOrganisationsHidden");
      eyes=json.get("eslNationalCaveats");
      atomal=json.get("eslAtomal");
      
      //Note that this is less logic than is in the client-side javascript
      //We're not trying to replicate the formatting here, just protect against XSS
      //and the formatting rules happen to make it easy
      freeformcaveats=freeformcaveats.replaceAll('[^a-zA-Z ]+','');
      
      if (closedmarkings!=null)
      {
          closedmarkings=closedmarkings+atomalToGroups(atomal, closedmarkings.length()>0);
      }
      else
      {
          closedmarkings=atomalToGroups(atomal, false);
      }
      
   
   // create the topic node, and add the first child node representing the topic text
   // NOTE: this is a change from the old web client, where the topic title was used as name
   //       for the topic node. We will use generated names to make sure we won't have naming
   //       clashes.
   var name = getUniqueChildName(forumNode, "post");
   var topicNode = forumNode.createNode(name, "fm:topic");

   // We use twice the same name for the topic and the post in it
   var contentNode = topicNode.createNode(name, "fm:post");
   contentNode.mimetype = "text/html";
   contentNode.properties.title = title;
   contentNode.content = content;

   //Set ESC properties
    //Iff we've got a PM (mandatory property of an ESL) then populate the whole ESL
    // both for the content node and the topic node
             if (pm != null && pm != "") {
                 contentNode.properties["es:nod"] = nod;
                 topicNode.properties["es:nod"] = nod;
                 contentNode.properties["es:pm"] = pm;
                 topicNode.properties["es:pm"] = pm;
                 contentNode.properties["es:freeFormCaveats"] = freeformcaveats;
                 topicNode.properties["es:freeFormCaveats"] = freeformcaveats;
                 contentNode.properties["es:nationalityCaveats"] = eyes;
                 topicNode.properties["es:nationalityCaveats"] = eyes;
                 if (closedmarkings != null && closedmarkings != "") {
                     contentNode.properties["es:closedMarkings"] = closedmarkings.split(",");
                     topicNode.properties["es:closedMarkings"] = closedmarkings.split(",");
                 }
                 else {
                     contentNode.properties["es:closedMarkings"] = null;
                     topicNode.properties["es:closedMarkings"] = null;
                 }

                 if (organisations != null && organisations != "") {
                     contentNode.properties["es:organisations"] = organisations.split(",");
                     topicNode.properties["es:organisations"] = organisations.split(",");
                 }
                 else {
                     contentNode.properties["es:organisations"] = null;
                     topicNode.properties["es:organisations"] = null;
                 }
                 topicNode.save(); //Only need to save topicNode if we have ESL, content node needs saving anyway
         }
   
   contentNode.save();

   // add the cm:syndication aspect
   var props = new Array();
   props["cm:published"] = new Date();
   contentNode.addAspect("cm:syndication", props);

   // add the tags to the topic node for now
   topicNode.tags = tags;
   
   // Set the node as perishable if required
   if(json.has("perishable")) {
	   var perishableReason = json.get("perishable");
	   
	   if(perishableReason) {
	     	 managedDeletion.setPerishable(topicNode, perishableReason);
	   }
   }

   return topicNode;
}

function main()
{
   // get requested node
   var node = getRequestNode();
   if (status.getCode() != status.STATUS_OK)
   {
      return;
   }

   ensureTagScope(node);

   var topicPost = createPost(node);
   
   model.postData = getTopicPostData(topicPost);
   
   // create an activity entry
   if (json.has("site") && json.has("page"))
   {
      var data =
      {
         title: model.postData.post.properties.title,
         page: json.get("page") + "?topicId=" + model.postData.topic.name
      }
      activities.postActivity("org.alfresco.discussions.post-created", json.get("site"), "discussions", jsonUtils.toJSONString(data));
   }   
}

main();
