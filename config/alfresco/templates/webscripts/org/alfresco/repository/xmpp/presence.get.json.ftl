{
    "presences" : [
        <#list presences as presence>
            {"user": "${presence.username}", "mode": "${presence.presence}", "status": "${presence.status}", "source":"${presence.source}"}<#if presence_has_next>,</#if>
        </#list>
     ]
}