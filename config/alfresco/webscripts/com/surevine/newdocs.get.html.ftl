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
</#macro>

<#macro getName item>
  <#if item.type='{http://www.alfresco.org/model/forum/1.0}topic'>
    ${item.children[0].properties["cm:title"]}
  <#elseif item.type='{http://www.alfresco.org/model/forum/1.0}post'>
   <@getName item.parent/>
  <#else>
    ${item.properties["cm:name"]}
  </#if>
</#macro>

<#-- lack of spacing needed to format url correctly-->
<#-- Recursivley navigate up the post tree until we find the topic node, and link to that-->
<#macro getTopic post><#if post.type='{http://www.alfresco.org/model/forum/1.0}topic'>${post.properties["cm:name"]}<#else><@getTopic post.parent/></#if></#macro>

<#macro renderUser user presence>  
		<#switch presence.availability>
		<#case "ONLINE">
			<#assign displayAvl = "online">
			<#break>
		<#case "BUSY">
			<#assign displayAvl = "busy">
			<#break>
		<#case "AWAY">
			<#assign displayAvl = "away">
			<#break>
		<#case "OFFLINE">
			<#assign displayAvl = "offline">
			<#break> 
		<#default>
			<#assign displayAvl = "unknown">
	</#switch>
	
	<div class="icon">
        <img src="/share/proxy/alfresco/sv-theme/user-profile/avatar?user=${user.properties["cm:userName"]}&size=smallAvatar" alt='${user.properties["cm:userName"]}'/>
    </div>
    
    <#if presence.availability == "UNKNOWN">
			<#assign btnTitle = 'Unable to retrieve status for ${user.properties["cm:firstName"]} ${user.properties["cm:lastName"]}.'>
	<#else>
			<#assign btnTitle = '${user.properties["cm:firstName"]} ${user.properties["cm:lastName"]} is ${displayAvl?html} in chat.'>
	</#if>			
	
    <div class="presence">
    
      	<#if presence.availability == "UNKNOWN" || presence.availability == "OFFLINE" || presence.serviceEnabled?string == "false">
	    		<button class="presence-indicator ${displayAvl}" type="button"  title="${btnTitle}" disabled="disabled">&nbsp;</button>
			<#else>
				<button class="presence-indicator ${displayAvl}" type="button"  title="${btnTitle}" onclick="Alfresco.thirdparty.presence.launchChat('${user.properties["cm:userName"]}','${presence.host}')">&nbsp;</button>
		</#if>
		
		<div class="presence-username">
		    <a  href="/share/page/user/${user.properties["cm:userName"]?url}/profile" class="theme-color-1" >${user.properties["cm:firstName"]} ${user.properties["cm:lastName"]}</a>
		 </div>
	
	</div>
</#macro>

<#-- As above, return the site the user is in-->
<#macro getSite item><#if item.type='{http://www.alfresco.org/model/site/1.0}site'>${item.properties["cm:name"]}<#else><@getSite item.parent/></#if></#macro>


      <#if results?size == 0>
      <div class="detail-list-item first-item last-item">
         <span>${msg("label.noItems")}</span>
      </div>
      <#else>
         <#list results as item>
            <#assign modifiedBy>
	         <#if item.modifier?exists>
	         	<@renderUser item.modifier item.modifierPresence/>
			 </#if>
            </#assign>
      <div class="detail-list-item <#if item_index = 0>first-item<#elseif !item_has_next>last-item</#if>">
         <div>
            
            <div class="details" style="padding: 0px">
               <div>
                  <#if item.commentedOn==true>
                     ${msg("text.commentedOn-by", modifiedBy)} <h4 class="inline"><a href="/share/<@getUrl item/>" class="theme-color-1"><@getName item.node/></a></h4> ${msg("text.commented-on", item.modTime?string("dd MMM, yyyy HH:mm:ss"))}
                  <#else>
                     ${msg("text.modified-by", modifiedBy)} <h4 class="inline"><a href="/share/<@getUrl item/>" class="theme-color-1"><@getName item.node/></a></h4> ${msg("text.modified-on", item.modTime?string("dd MMM, yyyy HH:mm:ss"))}
                  </#if>
               </div>
            </div>
         </div>
      </div>
         </#list>
      </#if>
