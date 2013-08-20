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
<#escape x as jsonUtils.encodeJSONString(x)>
{
<#if result.page??>
   <#assign page = result.page>
   "title": "<#if page.properties.title?exists>${page.properties.title}<#else>${page.name?replace("_", " ")}</#if>",
   "deletionState": "${result.deletionState}",
   "deletionAuthorisation": "${result.deletionAuthorisation}",
   "eslPM": "${result.eslPM}",
   "eslNod": "${result.eslNod}",
   "eslOpen": "<#if result.eslOpen?exists><#list result.eslOpen as eslOpen>${eslOpen}<#if eslOpen_has_next> </#if></#list></#if>",
   "eslOrganisations": "<#if result.eslOrganisations?exists><#list result.eslOrganisations as eslOrganisation>${eslOrganisation}<#if eslOrganisation_has_next> </#if></#list></#if>",
   "eslClosed": "<#if result.eslClosed?exists><#list result.eslClosed as eslClosed>${eslClosed}<#if eslClosed_has_next> </#if></#list></#if>",
   "eslFreeFormCaveats": "${result.eslFreeFormCaveats}",
   "eslEyes": "${result.eslEyes}",
   "eslAtomal": "${result.eslAtomal}",
   
   <#if page.hasAspect("md:markedForDeletion")>
   "archiveDue": "${page.properties['md:archivalDue']?datetime?string("EEE MMM dd yyyy")}",
   "deletedBy": "${page.properties['md:deletedBy']}",
   </#if>

   "pagetext": "${page.content}",
   "tags": [
   <#list result.tags as tag>
      "${tag}"<#if tag_has_next>,</#if>
   </#list>
   ],
   "links": [
   <#list result.links as link>
      "${link}"<#if link_has_next>,</#if>
   </#list>
   ],
   "pageList": [
   <#list result.pageList as p>
      "${p}"<#if p_has_next>,</#if>
   </#list>
   ],
   <#if page.hasAspect("cm:versionable")>
   "versionhistory": [
      <#list page.versionHistory as record>
   {
      "name": "${record.name}",
      "version": "${record.versionLabel}",
      "versionId": "${record.id}",
      "date": "${record.createdDate?datetime?string("yyyy-mm-dd'T'HH:MM:ss")}",
      "author": "${record.creator}"     
   }<#if record_has_next>,</#if>
      </#list> 
   ],
   </#if>  
   "permissions":
   {
      "create": ${result.container.hasPermission("CreateChildren")?string},
      "edit": ${page.hasPermission("Write")?string},
      "delete": ${page.hasPermission("Delete")?string}
   },
   "isDeletable":"${result.isDeletable}"
<#else>
   "error" : "${result.error!""}"
</#if>
}
</#escape>
