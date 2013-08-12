<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

/**
 * Returns the fm:post node given a fm:topic or fm:post node.
 * 
 * This function makes sure that a post is returned in case the passed node is a topic.
 */
function findPostNode(node)
{
   if (node.type == "{http://www.alfresco.org/model/forum/1.0}post")
   {
      return node;
   }
   else if (node.type == "{http://www.alfresco.org/model/forum/1.0}topic")
   {
      var nodes = getOrderedPosts(node);
      if (nodes.length > 0)
      {
         return nodes[0];
      }
      else
      {
         status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, "First post of topic node" + node.nodeRef + " missing");
      }
   }
   else
   {
      status.setCode(STATUS_BAD_REQUEST, "Incompatible node type. Required either fm:topic or fm:post. Received: " + node.type);
      return null;
   }
}

 
/** Returns the posts of a topic, ordered by creation date.
 * We use this for two things: To find the root node and the last reply
 */
function getOrderedPosts(topic)
{
   var query = " +TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\"" +
               " +PATH:\"" + topic.qnamePath + "/*\" ";
   var sortAttribute = "@{http://www.alfresco.org/model/content/1.0}published";
   return search.luceneSearch(topic.nodeRef.storeRef.toString(), query, sortAttribute, true) ;
}

/*
 * Returns a JavaScript object that is used by the freemarker template
 * to render a topic post
 */
function getTopicPostData(topicNode)
{  
   // fetch the posts
   var posts = getOrderedPosts(topicNode);
   
   return getTopicPostDataFromTopicAndPosts(topicNode, posts);
}

/*
 * Returns a JavaScript object that is used by the freemarker template
 * to render a topic post
 */
function getTopicPostDataFromTopicAndPosts(topicNode, posts)
{
   // check the first post (which is the main post)
   if (posts.length < 1)
   {
      status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, "First post of topic node" + topicNode.nodeRef + " missing");
      return;
   }
   
   var item = {};
   
   // fetch the data
   item.isTopicPost = true;
   item.topic = topicNode;
   item.post = posts[0];
   item.canEdit = canUserEditPost(item.post);
   item.author = people.getPerson(item.post.properties["cm:creator"]);
   item.authorPresence = presenceService.getUserPresence(item.post.properties["cm:creator"]);
   item.totalReplyCount = posts.length - 1;
   // in case of replies, find the last reply
   if (posts.length > 1)
   {
      item.lastReply = posts[posts.length - 1];
      item.lastReplyBy = people.getPerson(item.lastReply.properties["cm:creator"]);
      item.lastReplyByPresence = presenceService.getUserPresence(item.lastReply.properties["cm:creator"]);
   }
   
   if (item.topic && item.topic!=null)
   {
	   //Has the topic been marked for deletion?  If so, pass relevant properties into the model
	   var mdns="md:";
	   
	   var deletionState="notDeleted";
	   if (item.topic.hasAspect("md:deleted"))
	   {
          deletionState="deleted";
	   }
	   
	   var archiveDue = item.topic.properties[mdns+"archivalDue"];
	   {
		   if (archiveDue!=null)
		   {
		      item.archiveDue=archiveDue;
		      item.deletedBy=item.topic.properties[mdns+"deletedBy"];
		      deletionState="markedForDelete";
		   }
	   }
	   item.deletionState=deletionState;
	   
	   //(tc) here

	   indexOfSiteStart = item.topic.qnamePath.indexOf( 'cm:') + 3;
	   indexOfSiteEnd = item.topic.qnamePath.indexOf( '/', indexOfSiteStart);
	   var siteName = item.topic.qnamePath.substr( indexOfSiteStart, indexOfSiteEnd - indexOfSiteStart);
	   
	   item.isDeletable = "yes";
	   item.siteName = "foo";

	   if( siteName.length > 12) {
		  //could be a deletedItems site...

		   // eletedItems/cm

		  var siteNameSuffix = siteName.substr( siteName.length - 12);
		  item.siteName = siteNameSuffix;

    	  if( siteNameSuffix == "deletedItems" ) {
			   item.isDeletable = "no";
		   }
	   }

	   
	   /*
	   /app:company_home/st:sites/cm:sandboxdeletedItems/cm:discussions/cm:post-1307035146381_18
	   */
	   
	   var deletionAuthorisation="normal";
	   if (deletionState!="notDeleted" && isDeleter()) //As an optimisation, we leave deletionAuthorisation set to normal if the item isn't deleted as deleters and non-deleters have the same privs here
	   {
	      deletionAuthorisation="deleter";
	   }
	   item.deletionAuthorisation=deletionAuthorisation;
	   
	   item.archivalStatus = managedDeletion.getArchivalDetails(topicNode);
	   item.perishReason = managedDeletion.getPerishReason(topicNode.properties["md:perishReason"]);
  }

   // tags
   if (topicNode.tags != undefined)
   {
       item.tags = topicNode.tags;
   }
   else
   {
       item.tags = [];
   }

   var eslNS="{http://www.alfresco.org/model/enhancedSecurity/0.3}";
      var theESLNod=posts[0].properties[eslNS + "nod"];
      var theESLPM=posts[0].properties[eslNS + "pm"];
      var theESLFreeFormCaveats=posts[0].properties[eslNS + "freeFormCaveats"];
      var theESLClosed = new Array();
      theESLClosed = posts[0].properties[eslNS + "closedMarkings"];
      var seperatedClosed = seperateAtomalFromClosedMarkings(theESLClosed);
      theESLClosed = seperatedClosed.closedMarkings;
      var theESLAtomal = seperatedClosed.atomal;    
      
      var theESLOpen = new Array();
      theESLOpen = posts[0].properties[eslNS + "openMarkings"];
      
      var theESLOrganisations = new Array();
      theESLOrganisations = posts[0].properties[eslNS + "organisations"];
      
      var theESLEyes= posts[0].properties[eslNS + "nationalityCaveats"];
      
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
      if (theESLAtomal==null) {
          theESLAtomal="";
      }
         
      item.eslNod=theESLNod;
      item.eslPM=theESLPM;
      item.eslFreeFormCaveats=theESLFreeFormCaveats;
      item.eslClosed=theESLClosed;
      item.eslOpen=theESLOpen;
      item.eslEyes=theESLEyes;
      item.eslAtomal=theESLAtomal;
      item.eslOrganisations=theESLOrganisations;

   return item;
}

/**
 * Returns the data object that is used by the freemarker template to render a reply post
 */
function getReplyPostData(post)
{
   var item = {};
   item.isTopicPost = false;
   item.post = post;
   item.canEdit = canUserEditPost(item.post);
   item.author = people.getPerson(item.post.properties["cm:creator"]);
   item.authorPresence = presenceService.getUserPresence(item.post.properties["cm:creator"]);
   
   var eslNS="{http://www.alfresco.org/model/enhancedSecurity/0.3}";
   var theESLNod=post.properties[eslNS + "nod"];
   var theESLPM=post.properties[eslNS + "pm"];
   var theESLFreeFormCaveats=post.properties[eslNS + "freeFormCaveats"];
   var theESLClosed = new Array();
   theESLClosed = post.properties[eslNS + "closedMarkings"];
   var seperatedClosed = seperateAtomalFromClosedMarkings(theESLClosed);
   theESLClosed = seperatedClosed.closedMarkings;
   var theESLAtomal = seperatedClosed.atomal;    
      
   var theESLOpen = new Array();
   theESLOpen = post.properties[eslNS + "openMarkings"];
   var theESLOrganisations = new Array();
   theESLOrganisations = post.properties[eslNS + "organisations"];
   
   var theESLEyes= post.properties[eslNS + "nationalityCaveats"];
   
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
   if (theESLAtomal==null) {
       theESLAtomal="";
   }
      
   item.eslNod=theESLNod;
   item.eslPM=theESLPM;
   item.eslFreeFormCaveats=theESLFreeFormCaveats;
   item.eslClosed=theESLClosed;
   item.eslOpen=theESLOpen;
   item.eslEyes=theESLEyes;
   item.eslAtomal=theESLAtomal;
   item.eslOrganisations=theESLOrganisations;
   
   return item;
}

/**
 * Returns true if the current user can edit the post.
 * 
 * Site managers can edit any post, everyone else should only
 * be able to edit their own posts.
 */
function canUserEditPost(post)
{
   // see if user has write permission first of all
   var canEdit = post.hasPermission("Write");
   
   // if current user is not the same user as the author check
   // that they are a site manager otherwise they can't edit the post
   if (canEdit)
   {
      // get the site id
      var siteId = url.templateArgs.site;
      
      if (siteId !== null)
      {
         var username = person.properties["cm:userName"];
         var postAuthor = post.properties["cm:creator"];
   
         if (username != postAuthor)
         {
            canEdit = siteService.isSiteManager(siteId);
         }
      }
   }
   
   return canEdit;
}