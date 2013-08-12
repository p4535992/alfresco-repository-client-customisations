<#escape x as jsonUtils.encodeJSONString(x)>
{
	<#if archiveDetails.archivalDue??>
		archivalDue: "${archiveDetails.archivalDue?string("EEE MMM dd yyyy")}",
	</#if>
	<#if archiveDetails.archivalUser??>
		archivalUser: "${archiveDetails.archivalUser}",
	</#if>
	<#if archiveDetails.perishableReason??>
		perishableReason: "${archiveDetails.perishableReason}",
	</#if>
	status: "${archiveDetails.status}"
}
</#escape>