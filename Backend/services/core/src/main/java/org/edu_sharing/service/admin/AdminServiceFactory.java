package org.edu_sharing.service.admin;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public class AdminServiceFactory {	
	public static String HOME_APPLICATION_PROPERTIES="homeApplication.properties.xml";


	public static AdminService getInstance() throws NotAnAdminException{
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return ApplicationContextFactory.getApplicationContext().getBean(AdminServiceImpl.class);
	}
}
