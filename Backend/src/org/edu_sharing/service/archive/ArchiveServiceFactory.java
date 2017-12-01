package org.edu_sharing.service.archive;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class ArchiveServiceFactory {
	
	
	public static ArchiveService getArchiveService(String appId){
		
		if(!appId.equals(ApplicationInfoList.getHomeRepository().getAppId())){
			throw new RuntimeException("no remote version of ArchiveService implemented yet");
		}
		
		return new ArchiveServiceImpl();
	}
}
