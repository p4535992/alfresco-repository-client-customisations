package com.surevine.alfresco.repo.upload;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;

public class ScriptFilenameNormaliser extends BaseScopableProcessorExtension
		implements FilenameNormaliser {

	
	private FilenameNormaliser _impl;
	
	public void setImplementation(FilenameNormaliser impl)
	{
		_impl=impl;
	}
	
	@Override
	public String normalise(String filename) {
		return _impl.normalise(filename);
	}

}
