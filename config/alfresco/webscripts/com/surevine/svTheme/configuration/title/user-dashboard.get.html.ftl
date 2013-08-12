{
   "userDashboardTitleHTML": "<#if userDashboardTitleNode??>${jsonUtils.encodeJSONString(userDashboardTitleNode.content)}</#if>",
   "backgroundImage": {
   		nodeRef: "<#if backgroundImageNode??>${jsonUtils.encodeJSONString(backgroundImageNode.nodeRef)}</#if>",
   		title: "<#if backgroundImageNode??>${jsonUtils.encodeJSONString(backgroundImageNode.properties.title)}</#if>"
   	}
}