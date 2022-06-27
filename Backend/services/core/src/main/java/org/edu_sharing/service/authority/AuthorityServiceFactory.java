package org.edu_sharing.service.authority;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.spring.ApplicationContextFactory;

public class AuthorityServiceFactory {
	public static AuthorityService getAuthorityService(String applicationId){
		
		if(!applicationId.equals(ApplicationInfoList.getHomeRepository().getAppId()) &&
			!ApplicationInfoList.getRepositoryInfoById(applicationId).getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
			throw new RuntimeException("no remote version of AuthorityService implemented yet");
		}
		
		return getLocalService();
	}
	public static AuthorityService getLocalService(){
		//return new AuthorityServiceImpl();
		return (AuthorityService) ApplicationContextFactory.getApplicationContext().getBean("authorityService");
	}
}
