package org.edu_sharing.service.repoproxy;

import org.springframework.context.ApplicationContext;

public class RepoProxyFactory {

	static ApplicationContext applicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

	public static RepoProxy getRepoProxy() {
		return applicationContext.getBean("repoProxy", RepoProxy.class);
	}
}
