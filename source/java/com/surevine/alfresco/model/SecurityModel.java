package com.surevine.alfresco.model;

public class SecurityModel 
{
	
	private GroupCategory _open;
	private GroupCategory _closed;
	private GroupCategory _organisations;
	
	public SecurityModel()
	{
		_open = new GroupCategory();
		_closed = new GroupCategory();
		_organisations = new GroupCategory();
	}
	
	public GroupCategory getOpenMarkings()
	{
		return _open;
	}
	
	public GroupCategory getClosedMarkings()
	{
		return _closed;
	}
	
	public GroupCategory getOrganisations()
	{
		return _organisations;
	}
	
}