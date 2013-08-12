package com.surevine.alfresco.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class GroupCategory 
{
	
	private Collection<Group> _groupNames = new ArrayList<Group>();
	
	public Iterator<Group> getGroups()
	{
		return _groupNames.iterator();
	}
	
	public void addGroup(Group group)
	{
		if (group==null)
		{
			throw new NullPointerException("The group passed to addGroup cannot be null");
		}
		_groupNames.add(group);
	}
	
	public void addAllGroups(Collection<Group> groups)
	{
		Iterator<Group> i = groups.iterator();
		while (i.hasNext())
		{
			addGroup(i.next());
		}
	}
}