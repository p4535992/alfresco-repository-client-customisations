<#escape x as jsonUtils.encodeJSONString(x)>
{ 
   "result": 
    {
        "overallResult":"${machineResults.overallResult?string}",
        "constraints":
        [
        <#list machineResults.constraints as constraint>
            {
                "constraintName": "${constraint.constraintName}",
                "constraintAccessAllowed": "${constraint.result?string}",
                "values": 
                    [
                    <#list constraint.values as value>
                        {
                            "value": "${value.value}",
                            "accessAllowed": "${value.result?string}"
                        }
                        <#if value_has_next>,</#if>
                    </#list>
                    ]
            }<#if constraint_has_next>,</#if>
        </#list>
        ]
    }
}
</#escape>