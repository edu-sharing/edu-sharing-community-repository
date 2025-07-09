package org.edu_sharing.service.mediacenter;

import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public class MediacenterServiceFactory {
	
	public static MediacenterService getInstance() throws NotAnAdminException{
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return getLocalService();
	}
	public static MediacenterService getLocalService(){
		return ApplicationContextFactory.getApplicationContext().getBean(MediacenterServiceImpl.class);
	}

	public static MediacenterService getMediacenterService(String appId){
		return getLocalService();
	}

}
