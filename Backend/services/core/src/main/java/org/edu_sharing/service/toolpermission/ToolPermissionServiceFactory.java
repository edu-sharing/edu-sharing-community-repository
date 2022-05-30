package org.edu_sharing.service.toolpermission;

import org.edu_sharing.spring.ApplicationContextFactory;

public class ToolPermissionServiceFactory {
	public static ToolPermissionService getInstance(){
		return (ToolPermissionService) ApplicationContextFactory.getApplicationContext().getBean("toolPermissionService");
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
