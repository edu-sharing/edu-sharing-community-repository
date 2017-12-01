package org.edu_sharing.service.authority;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class AuthorityServiceFactory {
	public static AuthorityService getAuthorityService(String applicationId){
		
		if(!applicationId.equals(ApplicationInfoList.getHomeRepository().getAppId())){
			throw new RuntimeException("no remote version of SearchService implemented yet");
		}
		
		return new AuthorityServiceImpl();
	}
	public static AuthorityService getLocalService(){
		return new AuthorityServiceImpl();
	}
}
