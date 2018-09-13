package org.edu_sharing.service.register;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceImpl;

public class RegisterServiceFactory {

	
	public static RegisterService getRegisterService(String appId){

		RegisterService registerService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		
		if(appInfo.getPermissionService() == null || appInfo.getPermissionService().trim().equals("")){
			registerService = getLocalService();
		}else{
			try{
				Class clazz = Class.forName(appInfo.getPermissionService());
				Object obj = clazz.getConstructor(new Class[] { String.class}).newInstance(new Object[] { appId });
				registerService = (RegisterService)obj;
			}catch(Exception e){
				throw new RuntimeException(e.getMessage());
			}
		}
		
		return registerService;
	}

	public static RegisterService getLocalService() {
		// @TODO: We need to switch to an ldap service if ldap is enabled!
		return new RegisterServiceImpl();
	}
	
}
