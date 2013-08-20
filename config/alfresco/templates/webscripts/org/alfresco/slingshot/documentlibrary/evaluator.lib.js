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
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/enhanced-security/lib/enhanced-security.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/repository/delete/user-groups.lib.js">

var Evaluator =
{
   /**
    * Node Type evaluator
    */
   getNodeType: function Evaluator_getNodeType(node)
   {
      var nodeType = "";
      if (node.isContainer)
      {
         nodeType = "folder";
      }
      else if (node.typeShort == "app:folderlink")
      {
         nodeType = "folderlink";
      }
      else if (node.typeShort == "app:filelink")
      {
         nodeType = "filelink";
      }
      else
      {
         nodeType = "document";
      }
      return nodeType;
   },
   
   /**
    * Parent container evaluators
    */
   parentContainer: function Evaluator_parentContainer(node, permissions)
   {
   },
   
   /**
    * Document and Folder common evaluators
    */
   documentAndFolder: function Evaluator_documentAndFolder(node, permissions, status, actionLabels)
   {
      /* Simple Workflow */
      if (node.hasAspect("app:simpleworkflow"))
      {
         status["simple-workflow"] = true;
         if (node.properties["app:approveStep"] != null)
         {
            permissions["simple-approve"] = true;
            actionLabels["onActionSimpleApprove"] = node.properties["app:approveStep"];
         }
         if (node.properties["app:rejectStep"] != null)
         {
            permissions["simple-reject"] = true;
            actionLabels["onActionSimpleReject"] = node.properties["app:rejectStep"];
         }
      }
   },
   
   /**
    * Node Evaluator - main entrypoint
    */
   run: function Evaluator_run(node, isParent)
   {
      var nodeType = Evaluator.getNodeType(node),
         actions = {},
         actionSet = "empty",
         permissions = {},
         status = {},
         custom = {},
         actionLabels = {},
         activeWorkflows = [],
         createdBy = Common.getPerson(node.properties["cm:creator"]),
         modifiedBy = Common.getPerson(node.properties["cm:modifier"]),
         isLink = false,
         linkedNode = null,
         lockedBy = null,
         lockOwnerUser = "",
         eslPM = "",
         eslNod = "",
         eslFreeformCaveats="",
         eslEyes="",
         eslOpenMarkings="",
         eslOrganisations="",
         eslClosedMarkings="",
         eslAtomal="";

      /**
       * COMMON TO ALL
       */
      permissions =
      {
         "create": node.hasPermission("CreateChildren"),
         "edit": node.hasPermission("Write"),
         "delete": node.hasPermission("Delete"),
         "permissions": node.hasPermission("ChangePermissions"),
         "cancel-checkout": node.hasPermission("CancelCheckOut")
      };
      
      // When evaluating parent container
      if (isParent)
      {
         Evaluator.parentContainer(node, permissions);
      }

      // Get relevant actions set
      switch (nodeType)
      {
         /**
          * SPECIFIC TO: LINK
          */
         case "folderlink":
         case "filelink":
            actionSet = "link";
            isLink = true;
            
            /**
             * NOTE: After this point, the "node" object will be changed to a link's destination node
             *       if the original node was a filelink type.
             */
            linkedNode = node.properties.destination;
            if (linkedNode == null)
            {
               return null;
            }
            // Re-evaluate the nodeType based on the link's destination node
            nodeType = Evaluator.getNodeType(linkedNode);
            break;
         
         /**
          * SPECIFIC TO: FOLDER
          */
         case "folder":
            actionSet = "folder";

            /* Document Folder common evaluator */
            Evaluator.documentAndFolder(node, permissions, status, actionLabels);
            
            /* Rules applied? */
            if (node.hasAspect("rule:rules"))
            {
               status["rules"] = true;
            }

            /* Transferred Nodes */
            if (node.hasAspect("trx:transferred"))
            {
               status["transferred-node"] = true;
               permissions["view-source-repository"] = true;
               actionSet = "transferredFolder";
            }
            
            /* Deny delete permission if we have child folders or documents */
            var children = node.getChildren();
            for (var i = 0; i<children.length; i++) {
            	var childNode = children[i];
            	
            	if (childNode.typeShort == "cm:folder" || childNode.typeShort == "cm:content")
            	{
            		permissions["delete"] = false;
            		break;
            	}
            }
            
            // Show the mark for delete / remove mark / undo delete buttons appropriately for folders
            // This is slightly different logic to the file deletion permissions as folders don't actually
            // get moved to the deleted item site, they just get created and deleted as needed
            // (so don't get a deleted aspect)
       	 	var siteName = node.getSiteShortName();
        	
        	if( siteName.endsWith('deletedItems') ) {
        		if (isDeleter()) {
                	permissions["undoDelete"] = true;
        		} else {
        			throw "Permissions violation: 435236";
        		}
        	} else if (node.hasPermission("Write")) {
        		if (node.hasAspect("md:folderMarkedForDeletion") )
        		{
       				permissions["removeDeleteMark"] = true;

       				if (isDeleter())
        			{
        				permissions["deleteNow"] = true;
        			}
        		} else {
       				permissions["markForDelete"] = true;
    			}
            }
            
            break;

         /**
          * SPECIFIC TO: DOCUMENTS
          */
         case "document":
            actionSet = "document";
            
            /* Document Folder common evaluator */
            Evaluator.documentAndFolder(node, permissions, status, actionLabels);

            //Enhanced Security Metadata
             eslPM=node.properties["es:pm"];
             eslNod=node.properties["es:nod"];
             eslFreeformCaveats=node.properties["es:freeformcaveats"];
             if (eslFreeformCaveats==null)
             {
               eslFreeformCaveats="";
             }
             eslEyes=node.properties["es:nationalityCaveats"];
             
             eslClosedMarkings = new Array();
             eslClosedMarkings = node.properties["es:closedMarkings"];
             var seperatedClosed = seperateAtomalFromClosedMarkings(eslClosedMarkings);
             eslClosedMarkings = seperatedClosed.closedMarkings;
             eslAtomal = seperatedClosed.atomal;
             eslOpenMarkings = new Array();
             eslOpenMarkings = node.properties["es:openMarkings"];
             eslOrganisations = new Array();
             eslOrganisations = node.properties["es:organisations"];
             
            // Working Copy?
            if (node.hasAspect("cm:workingcopy"))
            {
               var wcStatus = "";
               lockedBy = Common.getPerson(node.properties["cm:workingCopyOwner"]);
               lockOwnerUser = lockedBy.userName;
               if (lockOwnerUser == person.properties.userName)
               {
                  wcStatus = "editing";
                  actionSet = "workingCopyOwner";
               }
               else
               {
                  wcStatus = "locked " + lockedBy.displayName + "|" + lockedBy.userName;
                  actionSet = "locked";
               }
               var wcNode = node.properties["source"];
               custom["isWorkingCopy"] = true;
               custom["workingCopyOriginal"] = wcNode.nodeRef;
               if (wcNode.hasAspect("cm:versionable") && wcNode.versionHistory !== null && wcNode.versionHistory.length > 0)
               {
                  custom["workingCopyVersion"] = wcNode.versionHistory[0].label;
               }
               permissions["view-original"] = true;

               // Google Doc?
               if (node.hasAspect("{http://www.alfresco.org/model/googledocs/1.0}googleResource"))
               {
                  custom["googleDocUrl"] = node.properties["gd:url"];
                  permissions["view-google-doc"] = true;
                  if (lockOwnerUser == person.properties.userName)
                  {
                     permissions["checkin-from-google"] = true;
                     wcStatus = "google-docs-owner";
                     actionSet = "googleDocOwner";
                  }
                  else
                  {
                     wcStatus = "google-docs-locked " + lockedBy.displayName + "|" + lockedBy.userName;
                     actionSet = "googleDocLocked";
                  }
               }
               status[wcStatus] = true;
            }
            // Locked?
            else if (node.isLocked && !node.hasAspect("trx:transferred"))
            {
               var lockStatus = "";
               lockedBy = Common.getPerson(node.properties["cm:lockOwner"]);
               lockOwnerUser = lockedBy.userName;
               if (lockOwnerUser == person.properties.userName)
               {
                  lockStatus = "lock-owner";
                  actionSet = "lockOwner";
               }
               else
               {
                  lockStatus = "locked " + lockedBy.displayName + "|" + lockedBy.userName;
                  actionSet = "locked";
               }
               var srcNodes = search.query(
               {
                  query: "+@cm\\:source:\"" + node.nodeRef + "\" +ISNOTNULL:cm\\:workingCopyOwner",
                  language: "lucene",
                  page:
                  {
                     maxItems: 1
                  }
               });
               if (srcNodes.length == 1)
               {
                  custom["hasWorkingCopy"] = true;
                  custom["workingCopyNode"] = srcNodes[0].nodeRef;
                  permissions["view-working-copy"] = true;

                  // Google Doc?
                  if (srcNodes[0].hasAspect("{http://www.alfresco.org/model/googledocs/1.0}googleResource"))
                  {
                     custom["googleDocUrl"] = srcNodes[0].properties["gd:url"];
                     permissions["view-google-doc"] = true;
                     if (lockOwnerUser == person.properties.userName)
                     {
                        permissions["checkin-from-google"] = true;
                        lockStatus = "google-docs-owner";
                        actionSet = "googleDocOwner";
                     }
                     else
                     {
                        lockStatus = "google-docs-locked " + lockedBy.displayName + "|" + lockedBy.userName;
                        actionSet = "googleDocLocked";
                     }
                  }
               }
               status[lockStatus] = true;
            }
            
            // Inline editable aspect?
            if (node.hasAspect("app:inlineeditable"))
            {
               permissions["inline-edit"] = true;
            }
            
            //Show the mark for delete / remove mark / undo delete buttons appropriatley
            if (node.hasAspect("md:deleted") && isDeleter())
            {
            	permissions["undoDelete"]=true;
            }
            else
            {
           	 	var siteName = node.parents[0].parents[0].name;
            	alreadyInDeletedItemsSite = false;
            	 if( siteName.endsWith('deletedItems') ) {
            		 alreadyInDeletedItemsSite = true;
            		 if (!isDeleter())
            		 {
            			 throw "Permissions violation: 435236";
            		 }
            	 }

              if (node.hasAspect("md:markedForDeletion") )
              {
            	 if( !alreadyInDeletedItemsSite) {
            		 permissions["removeDeleteMark"] = true;
            	 }
            	if (isDeleter())
            	{
            	  permissions["deleteNow"] = true;
            	}
              }
              else
              {
             	 if( !alreadyInDeletedItemsSite) {
             		 permissions["markForDelete"] = true;
             	 }
              }

              var siteName = node.getSiteShortName();
              var perishableReasons = managedDeletion.getPerishReasons(siteName);

              if (perishableReasons.size() > 0) {
                if (node.hasAspect("md:perishable")) {
                   if ( !alreadyInDeletedItemsSite) {
                       permissions["removePerishableMark"] = true;
                   }

                   if (isDeleter()) {
                           permissions["deleteNow"] = true;
                   }
                } else if (!alreadyInDeletedItemsSite) {
                   permissions["markAsPerishable"] = true;
                }
              }
            }
           
            
            // Google Docs editable aspect?
            if (node.hasAspect("{http://www.alfresco.org/model/googledocs/1.0}googleEditable"))
            {
               permissions["googledocs-edit"] = true;
            }

            /* Transferred Nodes */
            if (node.hasAspect("trx:transferred"))
            {
               status["transferred-node"] = true;
               permissions["view-source-repository"] = true;
               actionSet = "transferredDocument";
            }
            break;
      }
      
      if (node !== null)
      {
         // Part of an active workflow? Guard against stale worklow tasks.
         try
         {
            for each (activeWorkflow in node.activeWorkflows)
            {
               activeWorkflows.push(activeWorkflow.id);
            }
         }
         catch (e) {}
   
         return(
         {
            node: node,
            type: nodeType,
            isLink: isLink,
            linkedNode: linkedNode,
            status: status,
            actionSet: actionSet,
            actionPermissions: permissions,
            createdBy: createdBy,
            modifiedBy: modifiedBy,
            lockedBy: lockedBy,
            tags: node.tags,
            activeWorkflows: activeWorkflows,
            custom: jsonUtils.toJSONString(custom),
         	eslPM: eslPM,
         	eslNod: eslNod,
        	eslEyes: eslEyes,
         	eslFreeformCaveats: eslFreeformCaveats,
         	eslOpenMarkings: eslOpenMarkings,
         	eslOrganisations: eslOrganisations,
         	eslClosedMarkings: eslClosedMarkings,
         	eslAtomal: eslAtomal
         });
      }
      else
      {
         return null;
      }
   }
};
