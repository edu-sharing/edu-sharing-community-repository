package org.edu_sharing.service.register;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceImpl;

import java.io.InputStream;
import java.util.Properties;

public class RegisterServiceFactory {

	
	public static RegisterService getRegisterService(String appId) {
		return getLocalService();
	}
	public static RegisterService getLocalService() {
		try{
			Properties config = getConfig();
			Class clazz = Class.forName(RegisterServiceFactory.class.getPackage().getName()+"."+config.getProperty("class"));
			Object obj = clazz.newInstance();
			RegisterService registerService = (RegisterService) obj;
			return registerService;
		}catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}

	}
	public static Properties getConfig() throws Exception {
		return PropertiesHelper.getProperties("/org/edu_sharing/service/register/register.properties", PropertiesHelper.TEXT);
	}

}
