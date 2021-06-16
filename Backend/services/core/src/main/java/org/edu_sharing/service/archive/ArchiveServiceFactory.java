package org.edu_sharing.service.archive;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.spring.ApplicationContextFactory;

public class ArchiveServiceFactory {
	
	
	public static ArchiveService getArchiveService(String appId){
		
		if(!appId.equals(ApplicationInfoList.getHomeRepository().getAppId())){
			throw new RuntimeException("no remote version of ArchiveService implemented yet");
		}
		
		return new ArchiveServiceImpl();
	}

	public static ArchiveService getLocalService() {
		return new ArchiveServiceImpl();
	}
}
