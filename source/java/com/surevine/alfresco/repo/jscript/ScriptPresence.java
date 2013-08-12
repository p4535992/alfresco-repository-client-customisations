package com.surevine.alfresco.repo.jscript;

public class ScriptPresence {

	private String _status;
	
	private String _presence;
	
	private String _source;
	
	private String _username;
	
	public ScriptPresence(String status, String presence, String source, String username) {
		_status=status;
		_presence=presence;
		_source=source;
		_username=username;
	}
	
	public String getPresence() {
		return _presence;
	}
	
	public String getStatus() {
		return _status;
	}
	
	public String getSource() {
		return _source;
	}
	
	public String getUsername() {
		return _username;
	}
	
}
