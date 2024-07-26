package org.edu_sharing.service.tracking;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.DownloadServlet;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.InvocationTargetException;

public class TrackingServiceFactory {
    private static Logger logger = Logger.getLogger(TrackingServiceFactory.class);

    public static TrackingService getTrackingService() {
			return ApplicationContextFactory.getApplicationContext().getBean("trackingService", TrackingService.class);
	}

	public static TrackingServiceCustomInterface getTrackingServiceCustom() {
		try{
			return ApplicationContextFactory.getApplicationContext().getBean(TrackingServiceCustomInterface.class);
		}catch(NoSuchBeanDefinitionException ignored) {
			try {
				return (TrackingServiceCustomInterface) Class.forName(TrackingService.class.getName() + "Custom").getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException t) {
				logger.debug("no class " + TrackingService.class.getName() + "Custom" + " found, will use default implementation for tracking");
				return null;
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
        }
    }
}
