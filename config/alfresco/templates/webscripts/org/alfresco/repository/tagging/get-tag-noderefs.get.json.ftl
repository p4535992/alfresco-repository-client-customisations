<#escape x as jsonUtils.encodeJSONString(x)>

    {
        "tags":
        [
            <#list tags as tag>
            {
                "name": "${tag.name}",
                "nodeRef": "${tag.nodeRef}"
            }<#if tag_has_next>,</#if> 
            </#list>
        ]
    }

</#escape>