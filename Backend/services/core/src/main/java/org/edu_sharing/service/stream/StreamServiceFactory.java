package org.edu_sharing.service.stream;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

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
