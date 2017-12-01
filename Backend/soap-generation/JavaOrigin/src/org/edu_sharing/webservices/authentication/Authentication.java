package org.edu_sharing.webservices.authentication;

import org.edu_sharing.webservices.types.KeyValue;

public class Authentication {

	public AuthenticationResult authenticateByApp(java.lang.String applicationId, java.lang.String username, java.lang.String email, java.lang.String ticket, boolean createUser) throws AuthenticationException{
		return null;
	}
	
	public AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, java.lang.String ticket, KeyValue[] ssoData) throws AuthenticationException{
		return null;
	}
	
    public AuthenticationResult authenticateByCAS(java.lang.String username, java.lang.String proxyTicket) throws AuthenticationException{
    	return null;
    }
    public boolean checkTicket(java.lang.String username, java.lang.String ticket) throws AuthenticationException{
    	return false;
    }
    public AuthenticationResult authenticate(java.lang.String username, java.lang.String password) throws AuthenticationException{
    	return null;
    }	
    
}
