package com.surevine.alfresco.repo.impl;

import org.springframework.extensions.webscripts.ui.common.StringUtils;

public class CompleteHTMLStringUtils extends StringUtils {

	static {
		StringUtils.safeTags.add("HTML");
		StringUtils.safeTags.add("BODY");
		StringUtils.safeTags.add("HEAD");
	}
	
	public static String stripUnsafeHTMLTags(String unsanitisedHTML)
	{
		return StringUtils.stripUnsafeHTMLTags(unsanitisedHTML);
	}
	
}
