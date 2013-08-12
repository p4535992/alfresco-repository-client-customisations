package com.surevine.alfresco.repo;

public interface HTMLSanitiser {
	
	/**
	 * Take in a String of HTML, remove "unsafe" tags via some mechanism and return a safe version
	 * of the HTML
	 * @param unsanitisedHTML
	 * @return Sanitised version of the input HTML
	 */
	public String sanitiseHTMLString(String unsanitisedHTML);
	
}
