package org.edu_sharing.service.authentication;

import org.edu_sharing.spring.ApplicationContextFactory;

public class ScopeUserHomeServiceFactory {
	public static ScopeUserHomeService getScopeUserHomeService(){
		return (ScopeUserHomeService)ApplicationContextFactory.getApplicationContext().getBean("scopeUserHomeService");
	}
}
