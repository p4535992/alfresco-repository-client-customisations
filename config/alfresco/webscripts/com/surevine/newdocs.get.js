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
function changeCommentsToParentDocuments(listOfNodes)
{
    var out = new Array(listOfNodes.length);
    var outIdx=0;
    for (i=0; i < listOfNodes.length; i++)
    {
        try {
            node = listOfNodes[i];
            
            // We're not actually going to use either of the next three variables, but we do want to throw 
            // and catch an exception if there's an error retrieving them
            var modifier = people.getPerson(node.properties['cm:modifier']);
            var modifierFirstName = modifier.properties["cm:firstName"];
            var modifierSurname = modifier.properties["cm:lastName"];
            
            out[outIdx]=new Object();
            if (! (node.type=='{http://www.alfresco.org/model/forum/1.0}post' 
                && node.parent.name=='Comments' 
                && (node.parent.parent.parent.type=='{http://www.alfresco.org/model/content/1.0}content' || node.parent.parent.parent.type=='{http://www.alfresco.org/model/content/1.0}folder')) 
                )
            {
                out[outIdx].node=node;
                out[outIdx].modTime=node.properties['{http://www.alfresco.org/model/content/1.0}modified'];
                out[outIdx].commentedOn=false;
            }
            else
            {
              // If we drop in here the node is a comment
              var parentDocument = node.parent.parent.parent; 
              
              // Repeat above access checks, but for the root document as well
              var docModifier = people.getPerson(parentDocument.properties['cm:modifier']);
              var docModifierFirstName = docModifier.properties["cm:firstName"];
              var docModifierSurname = docModifier.properties["cm:lastName"];
                
              out[outIdx].node=parentDocument;
              out[outIdx].modTime=node.properties['{http://www.alfresco.org/model/content/1.0}modified'];
              out[outIdx].commentedOn=true;
            }
            
            // For the sake of attribution we want the modifier to be the commentor or the actual modifier.
            out[outIdx].modifier = people.getPerson(node.properties['cm:modifier']);
            
            // Request presence information for the modifier
            out[outIdx].modifierPresence = presenceService.getUserPresence(node.properties['cm:modifier']);

            outIdx++;
        }
        catch (error)
        {
            logger.log("Skipping node: "+listOfNodes[i]+" due to access issues. Caused by:");
            logger.log(error);
            continue;
        }
    }
    return trimToSize(out, outIdx);
}

//Initially implemented without this function and using push() at lines 13 and 21.  But the usage of push()
//seems not to preserve ordering, so having to use this slightly inelegant approach
function trimToSize(listOfNodes, size)
{
    var out = new Array(size);
    for (var i=0; i < size; i++)
    {
      out[i]=listOfNodes[i];
    }
    return out;
}

logger.log("Starting script Newdocs script");
var MAX_RESULTS=20;

var date = new Date();
var toQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();
date.setDate(date.getDate() - 3);
var fromQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();

var siteName=url.templateArgs.site;
var sitePart="/st:sites";
if (siteName!=null)
{
    sitePart +="/cm:"+siteName; 
}

var query = "+PATH:\"/app:company_home"+sitePart+"//*\" +@cm\\:modified:["+fromQuery+"T00\\:00\\:00 TO " 
            + toQuery + "T23\\:59\\:59]"
            +" -TYPE:\"fm:topic\" -TYPE:\"cm:folder\" -TYPE:\"cm:thumbnail\" -TYPE:\"act:compositeaction\" -TYPE:\"act:action\" -TYPE:\"cm:failedThumbnail\" -ASPECT:\"md:deleted\"";

logger.log("Starting Newdocs query: " + query);
var nodes = search.luceneSearch(query, "@cm:modified", false);

//Trim size of results to maximum before filtering
if (nodes.length > MAX_RESULTS)
{
    logger.log("Trimming results from "+nodes.length+" to "+MAX_RESULTS);
    nodes = trimToSize(nodes, MAX_RESULTS);
}

logger.log("Finished Newdocs query.  Starting filtration");
var filteredNodes = changeCommentsToParentDocuments(nodes);
logger.log("Newdocs nodes filtered");
model.results=filteredNodes;
model.resultsCount=filteredNodes.size;
