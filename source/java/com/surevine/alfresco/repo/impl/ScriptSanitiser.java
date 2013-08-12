package com.surevine.alfresco.repo.impl;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import com.surevine.alfresco.repo.HTMLSanitiser;

/**
 * Simple alternative implementation of HTMLSanitiser that exposes HTML sanitisation capabilities
 * to the Javascript API
 * @author SimonW
 *
 */
public class ScriptSanitiser extends BaseScopableProcessorExtension implements HTMLSanitiser {
	
	public String sanitiseHTMLString(String unsanitisedHTML)
	{
		if (unsanitisedHTML==null)
		{
			return "";
		}
		return CompleteHTMLStringUtils.stripUnsafeHTMLTags(unsanitisedHTML);
	}
	
	/**
	 * Convenience method with a shorter, more memorable name to make life easier in the javascript API
	 */
	public String sanitise(String s)
	{
		return sanitiseHTMLString(s);
	}

}
