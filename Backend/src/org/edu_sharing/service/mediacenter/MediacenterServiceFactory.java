package org.edu_sharing.service.mediacenter;

import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;

public class MediacenterServiceFactory {
	
	public static MediacenterService getInstance() throws NotAnAdminException{
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return new MediacenterServiceImpl();
	}
}
