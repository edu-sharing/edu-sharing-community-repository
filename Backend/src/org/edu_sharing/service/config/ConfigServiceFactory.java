package org.edu_sharing.service.config;

import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.service.config.model.Config;

public class ConfigServiceFactory {
	public static ConfigService getConfigService(){
		return new ConfigServiceImpl();
	}
	public static Config getCurrentConfig() throws Exception {
		try {
			return getConfigService().getConfigByDomain(getCurrentDomain());
		}catch(Throwable t) {
			return getConfigService().getConfig();
		}
		
	}
	public static String getCurrentDomain() {
		return Context.getCurrentInstance().getRequest().getServerName();
	}
}
