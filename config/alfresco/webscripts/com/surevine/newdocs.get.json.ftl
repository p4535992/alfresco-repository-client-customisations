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
<#macro dateFormat date>${date?string("'\"'dd MMM yyyy HH:mm:ss 'GMT'Z '('zzz')'\"")}</#macro>

<#macro getUrl item>
  <#if item.node.displayPath?contains("documentLibrary")>
    "page/site/<@getSite item.node/>/document-details?nodeRef=${item.node.nodeRef}"
  <#elseif item.node.displayPath?contains("wiki")>
    "page/site/<@getSite item.node/>/wiki-page?title=${item.node.properties["cm:name"]}"
  <#elseif item.node.displayPath?contains("discussions")>
    "page/site/<@getSite item.node/>/discussions-topicview?container=discussions&topicId=<@getTopic item.node/>&listViewLinkBack=true"
  <#else>
    "."
  </#if>
</#macro>

<#-- lack of spcaing needed to format url correctly-->
<#-- Recursivley navigate up the post tree until we find the topic node, and link to that-->
<#macro getTopic post><#if post.type='{http://www.alfresco.org/model/forum/1.0}topic'>${post.properties["cm:name"]}<#else><@getTopic post.parent/></#if></#macro>

<#-- As above, return the site the user is in-->
<#macro getSite item><#if item.type='{http://www.alfresco.org/model/site/1.0}site'>${item.properties["cm:name"]}<#else><@getSite item.parent/></#if></#macro>

<#escape x as jsonUtils.encodeJSONString(x)>
{
   "totalRecords": "${resultsCount}",
   "site": "${site}",
   "items": [
   	<#list results as item> { 
   		"displayName": "${item.node.properties["cm:name"]}",
   		"modifiedBy": "${item.node.properties["cm:modifier"]}",
   		"modifiedByLongName": "${people.getPerson(item.node.properties["cm:modifier"]).properties["cm:firstName"]+' '+people.getPerson(item.node.properties["cm:modifier"]).properties["cm:lastName"]}",
   		"modifiedOn": <@dateFormat item.modTime/>,
   		"url": <@getUrl item/>
        }<#if item_has_next>,</#if>
   	</#list>
   ]
}
</#escape>
