/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *   
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.surevine.alfresco.model;


public class Group 
{
	
	public enum DisplayType {
		GROUP("group", "Groups"),
		RESTRICTION("restriction", "Restrictions"), 
		ORGANISATION("organisation", "Organisations"), 
		THEMATIC("thematic", "Thematic Groups");
		
		private final String _ldapID;
		private final String _humanName;
		
		DisplayType(String ldapID, String humanName)
		{
			_ldapID=ldapID;
			_humanName=humanName;
		}
		
		public String getLdapID()
		{
			return _ldapID;
		}
		
		public String toString()
		{
			return _humanName;
		}
		
		public static DisplayType getDisplayType(String ldapID)
		{
			ldapID=ldapID.trim().toLowerCase();
			DisplayType[] values = DisplayType.values();
			for (int i=0; i <values.length; i++)
			{
				if (values[i]._ldapID.equals(ldapID))
				{
					return values[i];
				}
			}
			return null;
		}
	
	};
	
	private String _name="undefined";
	
	private String _humanName="undefined";
	
	private DisplayType _displayType=DisplayType.THEMATIC;
	
	private String _description="undefined";
	
	private String _permissionAuthoritySpec="undefined - undefined";
	
	private boolean _deprecated=false;
	
	public Group()
	{
		
	}
	
	public void setName(String name)
	{
		_name=name;
	}
	
	public void setHumanReadableName(String name)
	{
		_humanName=name;
	}
	
	public void setDisplayType(DisplayType type)
	{
		_displayType=type;
	}
	
	public void setDisplayType(String type)
	{
		_displayType=DisplayType.getDisplayType(type);
	}
	
	public void setDescription(String description)
	{
		_description=description;
	}
	
	public void setPermissionAuthoritySpecification(String spec)
	{
		_permissionAuthoritySpec=spec;
	}

	public String getName()
	{
		return _name;
	}
	
	public String getHumanReadableName()
	{
		return _humanName;
	}
	
	public DisplayType getDisplayType()
	{
		return _displayType;
	}
	
	public String getDescription()
	{
		return _description;
	}
	
	public String getPermissionAuthoritySpecification()
	{
		return _permissionAuthoritySpec;
	}
	
	public boolean isDeprecated()
	{
		return _deprecated;
	}
	
	public void setDeprecated(boolean deprecated)
	{
		_deprecated=deprecated;
	}
}
