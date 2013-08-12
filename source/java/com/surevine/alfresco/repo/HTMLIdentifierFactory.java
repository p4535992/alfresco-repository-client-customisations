package com.surevine.alfresco.repo;

/**
 * HTMLIdentifier is not guaranteed to be thread safe, so multi-threaded callers using dependancy injection
 * may wish to obtain instances using this factory 
 */
public interface HTMLIdentifierFactory {
	
	public HTMLIdentifier getHTMLIdentifier();

}
