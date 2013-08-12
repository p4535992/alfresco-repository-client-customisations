<#escape x as jsonUtils.encodeJSONString(x)>
{
	perishableReasons: [
	<#list perishableReasons as reason>
		{
			code: "${reason.code}",
			perishDays: "${reason.perishDays}",
			title: "${reason.title}",
			description: "${reason.description}"
		}<#if reason_has_next>,</#if>
	</#list>
	]
}
</#escape>