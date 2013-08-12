{
    "count": "${count}",
    "messages": [<#list messages as message>
                    {"userName": "${message.userName}", "displayName": "${message.displayName}", "count": "${message.count}"}<#if message_has_next>,</#if>
                </#list>],
    "invites": [<#list invites as invite>
                    { "groupChatName": "${invite.groupChatName}" }<#if invite_has_next>,</#if>
                </#list>]
}