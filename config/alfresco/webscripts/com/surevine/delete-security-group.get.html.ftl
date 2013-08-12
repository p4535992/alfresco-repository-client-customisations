<#macro renderGroupList list>
    <#list list as group>
    	<option>${group.name}</option>
    </#list>
</#macro>
<html>
	<head>
		<title>Delete Security Group</title>
	</head>
	<body>
		<h1>Delete Security Group</h2>
		<p>This is a test interface for functionality that will be provided by UMT.</p>
		
		<form method="POST" enctype="multipart/form-data" action="/alfresco/wcs/surevine/delete-security-group">
			<select name="group">
				<@renderGroupList openMarkings/>
				<@renderGroupList closedMarkings/>
				<@renderGroupList organisations/>
			</select>
		
			<input type="submit" value="Delete" />
		</form>
	</body>
</html>