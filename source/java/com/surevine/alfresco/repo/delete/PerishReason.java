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
package com.surevine.alfresco.repo.delete;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Represents a reason that an item may be perished
 */
public class PerishReason {
	private final String code;
	private final int perishDays;
	private final String title;
	private final String description;
	private final Set<String> sites;

	public PerishReason(String code, int perishDays, final String title, final String description, Set<String> sites) {
		super();
		this.code = code;
		this.perishDays = perishDays;
		this.title = title;
		this.description = description;
		this.sites = sites;
	}
	/**
	 * Gets the code for this reason.
	 * @return
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Returns the number of days before an item is perished.
	 * @return
	 */
	public int getPerishDays() {
		return perishDays;
	}

	/**
	 * Returns the short title of this reason
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns a longer description of this reason
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the set of sites to which this reason applies.
	 * @return
	 */
	public Set<String> getSites() {
		return sites;
	}

	/**
	 * Creates a new {@link PerishReason} instance from the given {@link JSONObject}.
	 * @param json the json representing the perish reason
	 * @return
	 * @throws JSONException
	 */
	public static PerishReason fromJSON(JSONObject json) throws JSONException {
		String code = json.getString("code");
		int perishDays = json.getInt("perishDays");
		String title = json.getString("title");
		String description = json.getString("description");
		JSONArray sites = json.getJSONArray("sites");
		
		HashSet<String> sitesSet = new HashSet<String>(sites.length());
		
		for(int i = 0; i < sites.length(); ++i) {
			sitesSet.add(sites.getString(i));
		}
		
		return new PerishReason(code, perishDays, title, description, sitesSet);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + perishDays;
		result = prime * result + ((sites == null) ? 0 : sites.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "PerishReason [code=" + code + ", perishDays=" + perishDays
				+ ", title=" + title + ", description=" + description
				+ ", sites=" + sites + "]";
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PerishReason))
			return false;
		PerishReason other = (PerishReason) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (perishDays != other.perishDays)
			return false;
		if (sites == null) {
			if (other.sites != null)
				return false;
		} else if (!sites.equals(other.sites))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
}
