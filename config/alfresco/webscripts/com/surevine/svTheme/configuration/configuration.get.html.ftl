{
   "changePasswordUrl": "<#if changePasswordUrlNode??>${jsonUtils.encodeJSONString(changePasswordUrlNode.content)}</#if>",
   "headerSecurityLabel": "<#if securityLabelHeaderNode??>${jsonUtils.encodeJSONString(securityLabelHeaderNode.content)}</#if>",
   "helpLinkUrl": "<#if helpLinkNode??>${jsonUtils.encodeJSONString(helpLinkNode.content)}</#if>",
   "launchChatUrl": "<#if launchChatUrlNode??>${jsonUtils.encodeJSONString(launchChatUrlNode.content)}</#if>",
   "appLogo": {
   		nodeRef: "<#if appLogoNode??>${jsonUtils.encodeJSONString(appLogoNode.nodeRef)}</#if>",
   		title: "<#if appLogoNode??>${jsonUtils.encodeJSONString(appLogoNode.properties.title)}</#if>"
   	},
   "surevineLinkUrl": "<#if surevineLinkUrlNode??>${jsonUtils.encodeJSONString(surevineLinkUrlNode.content)}</#if>"
}