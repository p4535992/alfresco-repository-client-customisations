<#macro dateFormat date>${date?string("dd MMM yyyy HH:mm:ss 'GMT'Z '('zzz')'")}</#macro>
<#escape x as jsonUtils.encodeJSONString(x)>
{
	"items":
	[
		<#list data.items as item>
		{
			"nodeRef": "${item.nodeRef}",
			"type": "${item.type}",
			"name": "${item.name!''}",
			"displayName": "${item.displayName!''}",
			<#if item.title??>
			"title": "${item.title}",
			</#if>
			"description": "${item.description!''}",
            <#if item.modifiedOn??> "modifiedOn": "<@dateFormat item.modifiedOn />", </#if>
			"modifiedByUser": "${item.modifiedByUser!''}",
			"modifiedBy": "${item.modifiedBy!''}",
			"isMarkedForDelete":"${item.isMarkedForDelete!'false'}",
			<#if item.markedForDeleteBy??>"markedForDeleteBy":"${item.markedForDeleteBy}",</#if>
			<#if item.archiveDueDate??>"archiveDueDate":"${item.archiveDueDate?string("EEE MMM dd yyyy")}",</#if>
            
			"isMarkedAsPerishable": "${item.isMarkedAsPerishable!'false'}",
			<#if item.perishDue??>"perishDue":"${item.perishDue?string("EEE MMM dd yyyy")}",</#if>
			<#if item.perishRequestedBy??>"perishRequestedBy":"${item.perishRequestedBy}",</#if>
			<#if item.perishReason??>"perishReason":"${item.perishReason}",</#if>
			<#if item.perishTitle??>"perishTitle":"${item.perishTitle}",</#if>
            <#if item.perishDescription??>"perishDescription":"${item.perishDescription}",</#if>

			<#if item.archivalStatus??>
		    "archivalStatus": {
		      <#if item.archivalStatus.archivalDue??>
			      "archivalDue": "${item.archivalStatus.archivalDue?datetime?string("EEE MMM dd yyyy")}",
			  </#if>
		      "status": "${item.archivalStatus.status}"
		    },
		    </#if>
		            
            <#if item.size??>"size": ${item.size?c},</#if>
			<#if item.site??>
			"site":
			{
				"shortName": "${item.site.shortName}",
				"title": "${item.site.title}"
			},
            <#if item.container??>"container": "${item.container}",</#if>
			</#if>
			<#if item.path??>
			"path": "${item.path}",
			</#if>
            "tags": [<#if item.tags??><#list item.tags as tag>"${tag}"<#if tag_has_next>,</#if></#list></#if>]
            <#if item.userName??>, "userName": "${item.userName}"</#if> 
            <#if item.firstName??>, "firstName": "${item.firstName}"</#if> 
            <#if item.lastName??>, "lastName": "${item.lastName}"</#if>
            <#if item.biography??>, "biography": "${item.biography}"</#if>
            <#if item.avatar??>, "avatar": "${item.avatar}"</#if>            
		}<#if item_has_next>,</#if>
		</#list>
	]
}
</#escape>