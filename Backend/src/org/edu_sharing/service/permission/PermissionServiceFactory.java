package org.edu_sharing.service.permission;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class PermissionServiceFactory {

	
	public static PermissionService getPermissionService(String appId){
		
		PermissionService permissionService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		
		if(appInfo.getPermissionService() == null || appInfo.getPermissionService().trim().equals("")){
			permissionService = new PermissionServiceImpl(appId);
		}else{
			try{
				Class clazz = Class.forName(appInfo.getPermissionService());
				Object obj = clazz.getConstructor(new Class[] { String.class}).newInstance(new Object[] { appId });
				permissionService = (PermissionService)obj;
			}catch(Exception e){
				throw new RuntimeException(e.getMessage());
			}
		}
		
		return permissionService;
	}
	
}
