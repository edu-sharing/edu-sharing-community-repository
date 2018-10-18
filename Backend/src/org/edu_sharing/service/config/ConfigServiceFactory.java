package org.edu_sharing.service.config;

import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.service.config.model.Config;

import javax.servlet.ServletRequest;

public class ConfigServiceFactory {
	public static ConfigService getConfigService(){
		return new ConfigServiceImpl();
	}
	public static Config getCurrentConfig() throws Exception {
		return getCurrentConfig(Context.getCurrentInstance().getRequest());
	}
	public static Config getCurrentConfig(ServletRequest req) throws Exception {
		try {
			return getConfigService().getConfigByDomain(getCurrentDomain());
		}catch(Throwable t) {
			return getConfigService().getConfig();
		}
		
	}
	public static String getCurrentDomain() {
		return getCurrentDomain(Context.getCurrentInstance().getRequest());
	}
	public static String getCurrentDomain(ServletRequest req) {
		return req.getServerName();
	}
}
