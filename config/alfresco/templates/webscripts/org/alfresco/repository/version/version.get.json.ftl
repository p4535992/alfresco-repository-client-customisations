<#escape x as jsonUtils.encodeJSONString(x)>
[
<#list versions as v>
	{
		"nodeRef": "${v.nodeRef}",
		"name": "${v.name}",
		"label": "${v.label}",
		"description": "${v.description}",
		"createdDate": "${v.createdDate?string("dd MMM yyyy HH:mm:ss 'GMT'Z '('zzz')'")}",
		"creator":
		{
			"userName": "${v.creator.userName}",
			"firstName": "${v.creator.firstName!""}",
			"lastName": "${v.creator.lastName!""}"
		},
		
		  <#if v.creatorPresence??>
            "creatorPresence":
	    	{
		        "availability": "${v.creatorPresence.availability!"UNKNOWN"}",
		        "status": "${v.creatorPresence.status!""}",
		    	"host":"${v.creatorPresence.host!""}",
		    	"serviceEnabled": "${v.creatorPresence.serviceEnabled?string}"
            }
        <#else>
	        "creatorPresence":
		    {
			    "availability": "UNKNOWN",
			    "status": "",
			    "host":"",
			    "serviceEnabled": "false"
       		 }
        </#if>
	}<#if (v_has_next)>,</#if>
</#list>
]
</#escape>