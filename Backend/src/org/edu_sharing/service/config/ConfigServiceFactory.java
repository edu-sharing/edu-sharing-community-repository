package org.edu_sharing.service.config;

public class ConfigServiceFactory {
	public static ConfigService getConfigService(){
		return new ConfigServiceImpl();
	}
}
