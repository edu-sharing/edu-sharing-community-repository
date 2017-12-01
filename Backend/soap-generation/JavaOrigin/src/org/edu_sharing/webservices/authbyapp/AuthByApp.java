package org.edu_sharing.webservices.authbyapp;

import org.edu_sharing.webservices.authentication.AuthenticationException;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.types.KeyValue;

public class AuthByApp {

	public AuthenticationResult authenticateByTrustedApp(String applicationId, KeyValue[] ssoData) throws AuthenticationException{
		return null;
	}
	
	public boolean checkTicket(String ticket) throws AuthenticationException{
    	return false;
    }
	
}
