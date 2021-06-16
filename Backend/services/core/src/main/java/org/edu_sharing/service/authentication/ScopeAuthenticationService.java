package org.edu_sharing.service.authentication;

public interface ScopeAuthenticationService {

	public String authenticate(String username, String password, String scope);
	
	boolean checkScope(String username, String scope);
	
	/**
	 * set scope in a threadlocal property out of user sessions(jsession,oauth)
	 */
	void setScopeForCurrentThread();

	/**
	 * How long is the session valid, in seconds.
	 * @return
	 */
	public int getSessionTimeout();
}
