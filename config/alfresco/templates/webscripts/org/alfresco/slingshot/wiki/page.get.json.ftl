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