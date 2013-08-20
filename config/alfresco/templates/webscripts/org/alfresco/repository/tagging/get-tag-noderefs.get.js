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
var storeRef="workspace://SpacesStore";

if (args.tagNames!=null && args.tagNames.length>0)
{
	var tagNames = args.tagNames.split(",");
	var nodeRefs = [];
	
	logger.log("Found "+tagNames.length+" tags within ["+args.tagNames+"]");
	
	for (var i=0; i < tagNames.length; i++)
	{
		if (tagNames[i].replace(/^\s+|\s+$/g,"").length>0)
		{
			var tagName = tagNames[i].replace(/^\s+|\s+$/g,"");
			var existingNode = taggingService.getTag(storeRef, tagName);
			var tagObj = {};
			tagObj.name=tagName;
			if (existingNode != null)
			{
				nodeRefStr = existingNode.getNodeRef().toString(); //Don't need the toString but might as well be explicit
				logger.log("For tag with name: "+tagName+" retrieving existing nodeRef: "+nodeRefStr);
			}
			else //Node doesn't exist so create it
			{
				nodeRefStr = taggingService.createTag(storeRef, tagNames[i]).getNodeRef().toString();
				logger.log("For new tag with name: "+tagName+" creating new nodeRef: "+nodeRefStr);
			}
			tagObj.nodeRef=nodeRefStr
			nodeRefs.push(tagObj);
		}
	}
	
	logger.log("Returning "+nodeRefs.length+" nodeRefs");
	
	model.tags=nodeRefs;
}
else
{
	logger.log("tagNames parameter empty or not present")
	model.tags = [];
}
