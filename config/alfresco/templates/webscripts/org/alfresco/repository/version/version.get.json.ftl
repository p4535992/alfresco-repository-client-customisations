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
