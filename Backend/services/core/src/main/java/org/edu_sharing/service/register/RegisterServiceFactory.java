package org.edu_sharing.service.register;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.util.Properties;

public class RegisterServiceFactory {

	
	public static RegisterService getRegisterService(String appId) {
		return getLocalService();
	}
	public static RegisterService getLocalService() {
		try{
			Config config = getConfig();
			Class clazz = Class.forName(RegisterServiceFactory.class.getPackage().getName()+"."+config.getString("class"));
			Object obj = clazz.newInstance();
			RegisterService registerService = (RegisterService) obj;
			return registerService;
		}catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}

	}
	public static Config getConfig() throws Exception {
		return LightbendConfigLoader.get().getConfig("repository.register");
	}

}
