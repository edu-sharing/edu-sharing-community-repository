package org.edu_sharing.service.toolpermission;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.connector.Connector;
import org.edu_sharing.service.connector.ConnectorList;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
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
