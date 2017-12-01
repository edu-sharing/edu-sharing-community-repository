/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.util.HashMap;

public class AuthenticatorRemoteAppResult {
	HashMap<String,String> authenticationInfo = null;
	String exceptionMessage = null;
	public HashMap<String, String> getAuthenticationInfo() {
		return authenticationInfo;
	}
	public void setAuthenticationInfo(HashMap<String, String> authenticationInfo) {
		this.authenticationInfo = authenticationInfo;
	}
	public String getExceptionMessage() {
		return exceptionMessage;
	}
	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}
	
}
