package com.surevine.alfresco.repo.impl;

import com.surevine.alfresco.repo.HTMLSanitiser;
import com.surevine.alfresco.repo.impl.CompleteHTMLStringUtils;

/**
 * Implementation of HTMLSanitiser that re-uses pre-existing Alfresco code to solve the sanitisation problem
 */
public class AlfrescoHTMLSanitiser implements HTMLSanitiser {

	public String sanitiseHTMLString(String unsanitisedHTML)
	{
		return CompleteHTMLStringUtils.stripUnsafeHTMLTags(unsanitisedHTML);
	}
	
}
