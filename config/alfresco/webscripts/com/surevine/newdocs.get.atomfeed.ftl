<!--
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
<#macro getSite item><#if item.type='{http://www.alfresco.org/model/site/1.0}site'>${item.properties["cm:name"]}<#else><@getSite item.parent/></#if></#macro>
<#macro encodepath node><#if node.parent?exists><@encodepath node=node.parent/>/${node.name?url}</#if></#macro>
<#macro getTopic post><#if post.type='{http://www.alfresco.org/model/forum/1.0}topic'>${post.properties["cm:name"]}<#else><@getTopic post.parent/></#if></#macro>
<#macro getUrl item>
<#compress>
<#if item.node.displayPath?contains("documentLibrary")>
<#if item.node.type=="{http://www.alfresco.org/model/content/1.0}folder">
page/site/<@getSite item.node/>/folder-details?nodeRef=${item.node.nodeRef}
<#else>
page/site/<@getSite item.node/>/document-details?nodeRef=${item.node.nodeRef}
</#if>
<#elseif item.node.displayPath?contains("wiki")>
page/site/<@getSite item.node/>/wiki-page?title=${item.node.properties["cm:name"]?url}
<#elseif item.node.displayPath?contains("discussions")>
page/site/<@getSite item.node/>/discussions-topicview?container=discussions&topicId=<@getTopic item.node/>&listViewLinkBack=true
<#else>
.
</#if>
</#compress>
</#macro>
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <generator version="${server.version}">Alfresco (${server.edition})</generator>
  <id>${absurl("/share/")}</id>
  <title>Recent Activities Site Feed</title>
  <updated>${xmldate(date)}</updated>
  <link rel="self" href="${absurl("/share/")}"/>
  <#list results as item>
  <entry xmlns="http://www.w3.org/2005/Atom">
  	<#assign modifiedBy>
	<#if item.modifier?exists>
		${item.modifier.properties["cm:firstName"]} ${item.modifier.properties["cm:lastName"]}
    <#else>
		"${item.node.properties["cm:modifier"]}"
	</#if>
	</#assign>
    <id>${absurl("/share/")}<@getUrl item/></id>
    <title>${item.node.name}</title>
    <updated>${xmldate(item.modTime)}</updated>
    <link rel="alternate" href="${absurl("/share/")}<@getUrl item/>"/>
    <summary>${item.node.name} <#if item.commentedOn==true>${msg("text.commentedOn-by", modifiedBy)}<#else>${msg("text.modified-by", modifiedBy)}</#if></summary>
    <author>
      <name>${item.node.properties.creator}</name>
    </author>
  </entry>
</#list>
</feed>
