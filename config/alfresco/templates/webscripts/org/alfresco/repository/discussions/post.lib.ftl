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
<#-- Renders a person object. -->
<#macro renderPerson person fieldName>
<#escape x as jsonUtils.encodeJSONString(x)>
    "${fieldName}":
    {
        <#if person.assocs["cm:avatar"]??>
        "avatarRef": "${person.assocs["cm:avatar"][0].nodeRef?string}",
        </#if>
        "username": "${person.properties["cm:userName"]}",
        "firstName": "${person.properties["cm:firstName"]!""}",
        "lastName": "${person.properties["cm:lastName"]!""}"
    },
</#escape>
</#macro>


<#macro addContent post>
<#escape x as jsonUtils.encodeJSONString(x)>
   <#assign safecontent=stringUtils.stripUnsafeHTML(post.content)>
    <#if (contentLength?? && contentLength > -1 && (safecontent?length > contentLength))>
        "content": "${safecontent?substring(0, contentLength)}",
    <#else>
        "content": "${safecontent}",
    </#if>
</#escape>
</#macro>


<#macro postJSON postData>
{
    <@postDataJSON postData=postData />
}
</#macro>

<#macro postDataJSON postData>
<#escape x as jsonUtils.encodeJSONString(x)>
    <#-- which node should be used for urls? which for the post data? -->
    <#if postData.isTopicPost>
        <#assign refNode=postData.topic />
    <#else>
        <#assign refNode=postData.post />
    </#if>
    
    <#assign post=postData.post />

    <#-- render topic post only data first -->
    <#if postData.isTopicPost>
        "name": "${postData.topic.name}",
        "totalReplyCount": ${postData.totalReplyCount?c},
        <#if postData.lastReply??>
            "lastReplyOn": "${postData.lastReply.properties.created?string("MMM dd yyyy HH:mm:ss 'GMT'Z '('zzz')'")}",
			<#if postData.lastReplyBy??>
            <@renderPerson person=postData.lastReplyBy fieldName="lastReplyBy" />
         <#else>
         "lastReplyBy":
         {
            "username": ""
         },
         </#if>
        </#if>
        <#if postData.lastReplyByPresence??>
            "lastReplyByPresence":
	    	{
		        "availability": "${postData.lastReplyByPresence.availability!"UNKNOWN"}",
		        "status": "${postData.lastReplyByPresence.status!""}",
		    	"host":"${postData.lastReplyByPresence.host!""}",
		    	"serviceEnabled": "${postData.lastReplyByPresence.serviceEnabled?string}"
            },
        <#else>
	        "lastReplyByPresence":
		    {
			    "availability": "UNKNOWN",
			    "status": "",
			    "host":"",
			    "serviceEnabled": "false"
       		 },
        </#if>
        "tags": [<#list postData.tags as x>"${x}"<#if x_has_next>, </#if></#list>],
    </#if>

    <#-- data using refNode which might be the topic or the post node -->
    "url": "/forum/post/node/${refNode.nodeRef.storeRef.protocol}/${refNode.nodeRef.storeRef.identifier}/${refNode.nodeRef.id}",
    "repliesUrl": "/forum/post/node/${refNode.nodeRef.storeRef.protocol}/${refNode.nodeRef.storeRef.identifier}/${refNode.nodeRef.id}/replies",
    "nodeRef": "${refNode.nodeRef}",
    
    <#-- data coming from the post node -->
    "title": "${(post.properties.title!"")}",
    "createdOn": "${post.properties.created?string("MMM dd yyyy HH:mm:ss 'GMT'Z '('zzz')'")}",
    "modifiedOn": "${post.properties.modified?string("MMM dd yyyy HH:mm:ss 'GMT'Z '('zzz')'")}",
    <#if (post.properties["cm:updated"]??)>
    "isUpdated": true,
    "updatedOn": "${post.properties["cm:updated"]?string("MMM dd yyyy HH:mm:ss 'GMT'Z '('zzz')'")}",
    <#else>
    "isUpdated": false,   
    </#if>
    <#if postData.author??>
        <@renderPerson person=postData.author fieldName="author" />
    <#else>
        "author":
        {
            "username": "${post.properties["cm:creator"]}"
        },
    </#if>
    <#if postData.authorPresence??>
        "authorPresence":
		{
		    "availability": "${postData.authorPresence.availability!"UNKNOWN"}",
		    "status": "${postData.authorPresence.status!""}",
		    "host":"${postData.authorPresence.host!""}",
		    "serviceEnabled":"${postData.authorPresence.serviceEnabled?string}"
        },
    <#else>
	    "authorPresence":
		{
		    "availability": "UNKNOWN",
		    "status": "",
		    "serviceEnabled": "false"
        },
    </#if>   
    <@addContent post=post />
    "replyCount": <#if post.sourceAssocs["cm:references"]??>${post.sourceAssocs["cm:references"]?size?c}<#else>0</#if>,
    <#if postData.deletionState?exists>
      "deletionState": "${postData.deletionState}",
    </#if>
    <#if postData.deletionAuthorisation?exists>
      "deletionAuthorisation": "${postData.deletionAuthorisation}",
    </#if>
    "permissions":
    {
        "edit": ${postData.canEdit?string},
        "reply": ${post.parent.hasPermission("CreateChildren")?string},
        "delete": ${post.hasPermission("Delete")?string}
    },
    <#if postData.archiveDue?exists>
    "archiveDue":"${postData.archiveDue?datetime?string("EEE MMM dd yyyy")}",
    "deletedBy":"${postData.deletedBy}",
    </#if>
    <#if postData.isTopicPost && postData.topic.hasAspect("md:perishable")>
    "perishDue": "${postData.topic.properties['md:perishDue']?datetime?string("EEE MMM dd yyyy")}",
    "perishRequestedBy": "${postData.topic.properties['md:perishRequestedBy']}",
    "perishReason": "${postData.topic.properties['md:perishReason']}",
	   <#if postData.perishReason??>
	   "perishTitle": "${postData.perishReason.title}",
	   "perishDescription": "${postData.perishReason.description}",
	   </#if>
    </#if>
    
    <#if postData.archivalStatus?exists>
    "archivalStatus": {
      <#if postData.archivalStatus.archivalDue??>
	      "archivalDue": "${postData.archivalStatus.archivalDue?datetime?string("EEE MMM dd yyyy")}",
	  </#if>
      "status": "${postData.archivalStatus.status}"
    },
	</#if>
    "isDeletable":"${postData.isDeletable!""}",
    "siteName":"${postData.siteName!""}",

    "eslPM": "<#if postData.eslPM?exists>${postData.eslPM}<#else> </#if>",
    "eslNod": "<#if postData.eslNod?exists>${postData.eslNod}<#else> </#if>",
    "eslFreeFormCaveats": "<#if postData.eslFreeFormCaveats?exists>${postData.eslFreeFormCaveats}<#else> </#if>",
    "eslEyes": "<#if postData.eslEyes?exists>${postData.eslEyes}<#else> </#if>",
    "eslOpen": "<#if postData.eslOpen?exists><#list postData.eslOpen as eslOpen>${eslOpen}<#if eslOpen_has_next> </#if></#list></#if>",
    "eslOrganisations": "<#if postData.eslOrganisations?exists><#list postData.eslOrganisations as eslOrganisation>${eslOrganisation}<#if eslOrganisation_has_next> </#if></#list></#if>",
    "eslClosed": "<#if postData.eslClosed?exists><#list postData.eslClosed as eslClosed>${eslClosed}<#if eslClosed_has_next> </#if></#list></#if>",
    "eslAtomal": "<#if postData.eslAtomal?exists>${postData.eslAtomal}<#else> </#if>"
</#escape>
</#macro>


<#-- Renders replies.
    The difference is to a normal post is that the children might be
    added inline in the returned JSON.
-->
<#macro repliesJSON data>
{
    <@postDataJSON postData=data />
    <#if data.children?exists>
        , "children": <@repliesRootJSON children=data.children />
    </#if>
}
</#macro>

<#macro repliesRootJSON children>
[
    <#list children as child>
        <@repliesJSON data=child/>
        <#if child_has_next>,</#if>
    </#list>
]
</#macro>
