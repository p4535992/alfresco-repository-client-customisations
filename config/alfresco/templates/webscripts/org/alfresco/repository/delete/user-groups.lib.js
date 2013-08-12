function isDeleter()
{
	var userName=person.properties.userName;
	
	var groupMembers = groups.getGroup('Deleters').getAllUsers();
	
	for (var i=0; i < groupMembers.length; i++)
	{
		if (userName==groupMembers[i].getShortName())
		{
			return true;
		}
	}
	return false;
	
}