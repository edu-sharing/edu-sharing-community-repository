package org.edu_sharing.service.stream;

import com.typesafe.config.Config;
import org.edu_sharing.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.register.RegisterService;

import java.util.Properties;

public class StreamServiceFactory {
	public static StreamService getStreamService() {
		try {
			Config config = getConfig();
			Class clazz = Class.forName(StreamServiceFactory.class.getPackage().getName()+"."+config.getString("class"));
			return (StreamService) clazz.newInstance();
		}catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}
	public static Config getConfig() throws Exception {
		return LightbendConfigLoader.get().getConfig("stream");
	}
}
