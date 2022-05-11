package org.edu_sharing.service.organization;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class OrganizationServiceFactory {
	public static OrganizationService getOrganizationService(String applicationId){
		
		if(!applicationId.equals(ApplicationInfoList.getHomeRepository().getAppId())){
			throw new RuntimeException("no remote version of SearchService implemented yet");
		}
		
		return new OrganizationServiceImpl();
	}
}
