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

/** Name used for the topic that contains all comments. */
const COMMENTS_TOPIC_NAME = "Comments";

/**
 * Returns all comment nodes for a given node.
 * @return an array of comments.
 */
function getComments(node)
{
   var commentsFolder = getCommentsFolder(node);
   if (commentsFolder !== null)
   {
      var elems = commentsFolder.childAssocs["cm:contains"];
      if (elems !== null)
      {
         return elems;
      }
   }
   // no comments found, return an empty array
   return [];
}

/**
 * Returns the folder that contains all the comments.
 *
 * We currently use the fm:discussable aspect where we
 * add a "Comments" topic to it.
 */
function getCommentsFolder(node)
{
   if (node.hasAspect("fm:discussable"))
   {
      var forumFolder = node.childAssocs["fm:discussion"][0];
      var topicFolder = forumFolder.childByNamePath(COMMENTS_TOPIC_NAME);
      return topicFolder;
   }
   else
   {
      return null;
   }
}

/**
 * Creates the comments folder if it doesn't yet exist for the given node.
 */
function getOrCreateCommentsFolder(node)
{
   var commentsFolder = getCommentsFolder(node);
   if (commentsFolder == null)
   {
      commentsFolder = commentService.createCommentsFolder(node);
   }
   return commentsFolder;
}

/**
 * Returns the data object for a comment node
 */
function getCommentData(node)
{
   var data = {};
   data.node = node;

   data.author = people.getPerson(node.properties["cm:creator"]);
   var userName = data.author.properties["cm:userName"];
   data.presence = presenceService.getUserPresence(userName);
   data.isUpdated = (node.properties["cm:modified"] - node.properties["cm:created"]) > 5000;
   return data;
}

/**
 * Returns the count of comments for a node.
 */
function getCommentsCount(node)
{
   return getComments(node).length;
}
