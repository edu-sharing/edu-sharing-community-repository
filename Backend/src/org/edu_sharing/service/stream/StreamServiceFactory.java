package org.edu_sharing.service.stream;

import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.register.RegisterService;

import java.util.Properties;

public class StreamServiceFactory {
	public static StreamService getStreamService() {
		try {
			Properties config = getConfig();
			Class clazz = Class.forName(StreamServiceFactory.class.getPackage().getName()+"."+config.getProperty("class"));
			return (StreamService) clazz.newInstance();
		}catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}
	public static Properties getConfig() throws Exception {
		return PropertiesHelper.getProperties(getConfigFile(), PropertiesHelper.TEXT);
	}

	public static String getConfigFile() {
		return "/org/edu_sharing/service/stream/stream.properties";
	}

}
