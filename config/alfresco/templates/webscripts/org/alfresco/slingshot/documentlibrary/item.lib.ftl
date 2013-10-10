<#--
    Copyright (C) 2008-2010 Surevine Limited.
      
    Although intended for deployment and use alongside Alfresco this module should
    be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
    http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
    
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<#assign workingCopyLabel = " " + message("coci_service.working_copy_label")>

<#macro dateFormat date=""><#if date?is_date>${xmldate(date)}</#if></#macro>

<#macro itemJSON item>
   <#escape x as jsonUtils.encodeJSONString(x)>
      <#assign node = item.node>
      <#assign version = "1.0">
      <#if node.hasAspect("cm:versionable") && node.versionHistory?size != 0><#assign version = node.versionHistory[0].versionLabel></#if>
      <#if item.createdBy??>
         <#assign createdBy = item.createdBy.displayName>
         <#assign createdByUser = item.createdBy.userName>
      <#else>
         <#assign createdBy="" createdByUser="">
      </#if>
      <#if item.modifiedBy??>
         <#assign modifiedBy = item.modifiedBy.displayName>
         <#assign modifiedByUser = item.modifiedBy.userName>
      <#else>
         <#assign modifiedBy="" modifiedByUser="">
      </#if>
      <#if item.lockedBy??>
         <#assign lockedBy = item.lockedBy.displayName>
         <#assign lockedByUser = item.lockedBy.userName>
      <#else>
         <#assign lockedBy="" lockedByUser="">
      </#if>
      <#assign tags><#list item.tags as tag>"${tag}"<#if tag_has_next>,</#if></#list></#assign>
   "nodeRef": "${node.nodeRef}",
   "nodeType": "${shortQName(node.type)}",
   "type": "${item.type}",
   "archivalStatus": {
      <#if item.archivalStatus.archivalDue??>
	      "archivalDue": "${item.archivalStatus.archivalDue?datetime?string("EEE MMM dd yyyy")}",
	  </#if>
      "status": "${item.archivalStatus.status}"
   },
   <#if node.hasAspect("md:markedForDeletion")>
   "archiveDue": "${node.properties['md:archivalDue']?datetime?string("EEE MMM dd yyyy")}",
   "deletedBy": "${node.properties['md:deletedBy']}",
   </#if>
   <#if node.hasAspect("md:folderMarkedForDeletion")>
   "folderArchiveDue": true,
   "folderDeletedBy": "${node.properties['md:folderDeletedBy']}",
   </#if>
   <#if node.hasAspect("md:perishable")>
   "perishDue": "${node.properties['md:perishDue']?datetime?string("EEE MMM dd yyyy")}",
   "perishRequestedBy": "${node.properties['md:perishRequestedBy']}",
   "perishReason": "${node.properties['md:perishReason']}",
	   <#if item.perishReason??>
	   "perishTitle": "${item.perishReason.title}",
	   "perishDescription": "${item.perishReason.description}",
	   </#if>
   </#if>
   "mimetype": "${node.mimetype!""}",
   "isFolder": <#if item.linkedNode??>${item.linkedNode.isContainer?string}<#else>${node.isContainer?string}</#if>,
   "isLink": ${(item.isLink!false)?string},
<#if item.linkedNode??>
   "linkedNodeRef": "${item.linkedNode.nodeRef?string}",
</#if>
   "fileName": "<#if item.linkedNode??>${item.linkedNode.name}<#else>${node.name}</#if>",
   "displayName": "<#if item.linkedNode??>${item.linkedNode.name}<#elseif node.hasAspect("cm:workingcopy")>${node.name?replace(workingCopyLabel, "")}<#else>${node.name}</#if>",
   "status": "<#list item.status?keys as s><#if item.status[s]?is_boolean && item.status[s] == true>${s}<#if s_has_next>,</#if></#if></#list>",
   "title": "${node.properties.title!""}",
   "description": "${node.properties.description!""}",
   "author": "${node.properties.author!""}",
   "createdOn": "<@dateFormat node.properties.created />",
   "createdBy": "${createdBy}",
   "createdByUser": "${createdByUser}",
   "modifiedOn": "<@dateFormat node.properties.modified />",
   "modifiedBy": "${modifiedBy}",
   "modifiedByUser": "${modifiedByUser}",
   "lockedBy": "${lockedBy}",
   "lockedByUser": "${lockedByUser}",
   "size": "${node.size?c}",
   "version": "${version}",
   "contentUrl": "api/node/content/${node.storeType}/${node.storeId}/${node.id}/${node.name?url}",
   "webdavUrl": "${node.webdavUrl}",
   "actionSet": "${item.actionSet}",
   <#if item.type=="document">
     "eslPM": "${item.eslPM!""}",
     "eslFreeformCaveats": "${item.eslFreeformCaveats!""}",
     "eslNod": "${item.eslNod!""}",
     "eslAtomal": "${item.eslAtomal!""}",
     "eslEyes": "${item.eslEyes!""}",
     "eslOpenMarkings": "<#if item.eslOpenMarkings?exists><#list item.eslOpenMarkings as eslOpen>${eslOpen}<#if eslOpen_has_next> </#if></#list></#if>",
     "eslOrganisations": "<#if item.eslOrganisations?exists><#list item.eslOrganisations as eslOrganisation>${eslOrganisation}<#if eslOrganisation_has_next> </#if></#list></#if>",
     "eslClosedMarkings": "<#if item.eslClosedMarkings?exists><#list item.eslClosedMarkings as eslClosed>${eslClosed}<#if eslClosed_has_next> </#if></#list></#if>",
   </#if>
     <#if item.modifiedByUserPresence??>
        "modifiedByUserPresence":
		{
		    "availability": "${item.modifiedByUserPresence.availability!"UNKNOWN"}",
		    "status": "${item.modifiedByUserPresence.status!""}",
		    "host":"${item.modifiedByUserPresence.host!""}",
		    "serviceEnabled": "${item.modifiedByUserPresence.serviceEnabled?string}"
        },
    <#else>
	    "modifiedByUserPresence":
		{
		    "availability": "UNKNOWN",
		    "status": "",
		    "serviceEnabled": "false"
        },
    </#if>    
     <#if item.createdByUserPresence??>
        "createdByUserPresence":
		{
		    "availability": "${item.createdByUserPresence.availability!"UNKNOWN"}",
		    "status": "${item.createdByUserPresence.status!""}",
		    "host":"${item.createdByUserPresence.host!""}",
		    "serviceEnabled": "${item.createdByUserPresence.serviceEnabled?string}"
        },
    <#else>
	    "createdByUserPresence":
		{
		    "availability": "UNKNOWN",
		    "status": "",
		    "serviceEnabled": "false"
        },
    </#if>   
   "tags": <#noescape>[${tags}]</#noescape>,
   "categories": [<#list node.properties.categories![] as c>["${c.name}", "${c.displayPath?replace("/categories/General","")}"]<#if c_has_next>,</#if></#list>],
   <#if item.activeWorkflows??>"activeWorkflows": "<#list item.activeWorkflows as aw>${aw}<#if aw_has_next>,</#if></#list>",</#if>
   <#if item.isFavourite??>"isFavourite": ${item.isFavourite?string},</#if>
   "location":
   {
      "repositoryId": "${(node.properties["trx:repositoryId"])!(server.id)}",
      "site": "${item.location.site!""}",
      "siteTitle": "${item.location.siteTitle!""}",
      "container": "${item.location.container!""}",
      "path": "${item.location.path!""}",
      "file": "${item.location.file!""}",
      "parent":
      {
      <#if item.location.parent??>
         <#if item.location.parent.nodeRef??>
         "nodeRef": "${item.location.parent.nodeRef!""}"
         </#if>
      </#if>      
      }
   },
   <#if node.hasAspect("cm:geographic")>"geolocation":
   {
      "latitude": ${(node.properties["cm:latitude"]!0)?c},
      "longitude": ${(node.properties["cm:longitude"]!0)?c}
   },</#if>
   <#if node.hasAspect("exif:exif")>"exif":
   {
      "dateTimeOriginal": "<@dateFormat node.properties["exif:dateTimeOriginal"] />",
      "pixelXDimension": ${(node.properties["exif:pixelXDimension"]!0)?c},
      "pixelYDimension": ${(node.properties["exif:pixelYDimension"]!0)?c},
      "exposureTime": ${(node.properties["exif:exposureTime"]!0)?c},
      "fNumber": ${(node.properties["exif:fNumber"]!0)?c},
      "flash": ${(node.properties["exif:flash"]!false)?string},
      "focalLength": ${(node.properties["exif:focalLength"]!0)?c},
      "isoSpeedRatings": "${node.properties["exif:isoSpeedRatings"]!""}",
      "manufacturer": "${node.properties["exif:manufacturer"]!""}",
      "model": "${node.properties["exif:model"]!""}",
      "software": "${node.properties["exif:software"]!""}",
      "orientation": ${(node.properties["exif:orientation"]!0)?c},
      "xResolution": ${(node.properties["exif:xResolution"]!0)?c},
      "yResolution": ${(node.properties["exif:yResolution"]!0)?c},
      "resolutionUnit": "${node.properties["exif:resolutionUnit"]!""}"
   },</#if>
   "permissions":
   {
      "inherited": ${node.inheritsPermissions?string},
      "roles":
      [
      <#list node.fullPermissions as permission>
         "${permission?string}"<#if permission_has_next>,</#if>
      </#list>
      ],
      "userAccess":
      {
      <#list item.actionPermissions?keys as actionPerm>
         <#if item.actionPermissions[actionPerm]?is_boolean>
         "${actionPerm?string}": ${item.actionPermissions[actionPerm]?string}<#if actionPerm_has_next>,</#if>
         </#if>
      </#list>
      }
   },
   <#if item.custom??>"custom": <#noescape>${item.custom}</#noescape>,</#if>
   "actionLabels":
   {
<#if item.actionLabels??>
   <#list item.actionLabels?keys as actionLabel>
      "${actionLabel?string}": "${item.actionLabels[actionLabel]}"<#if actionLabel_has_next>,</#if>
   </#list>
</#if>
   }
   </#escape>
</#macro>
