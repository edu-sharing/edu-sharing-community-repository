package org.edu_sharing.service.toolpermission;

import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

public class ToolPermissionServiceFactory {
	public static ToolPermissionService getInstance(){

		ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();
		// the context isn't set on startup, so we need to return null
		if( applicationContext == null){
			return null;
		}

		return applicationContext.getBean("toolPermissionService", ToolPermissionService.class);
		/*
		if( tps == null){
			 tps = (ToolPermissionService) ApplicationContextFactory.getApplicationContext().getBean("toolPermissionService");
			 			
			try{
				tps.initToolPermissions(getAllPredefinedToolPermissions());
			}catch(Throwable e){
				logger.error(e.getMessage(),e);
				return null;
			}
		}
		return tps;
		*/
	}
}
