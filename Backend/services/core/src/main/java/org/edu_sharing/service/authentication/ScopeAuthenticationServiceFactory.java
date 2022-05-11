package org.edu_sharing.service.authentication;

import org.edu_sharing.spring.ApplicationContextFactory;

public class ScopeAuthenticationServiceFactory {

	public static ScopeAuthenticationService getScopeAuthenticationService(){
		ScopeAuthenticationService sas = (ScopeAuthenticationService)ApplicationContextFactory.getApplicationContext().getBean("scopeAuthenticationService");
		return sas;
	}
	
}
