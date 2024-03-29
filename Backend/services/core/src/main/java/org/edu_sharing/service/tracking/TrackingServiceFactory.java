package org.edu_sharing.service.tracking;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.DownloadServlet;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.spring.ApplicationContextFactory;

public class TrackingServiceFactory {
    private static Logger logger = Logger.getLogger(TrackingServiceFactory.class);

    public static TrackingService getTrackingService() {
			return (TrackingService) ApplicationContextFactory.getApplicationContext().getBean("trackingService");
	}

	public static TrackingServiceCustomInterface getTrackingServiceCustom() {
		try {
			return (TrackingServiceCustomInterface) Class.forName(TrackingService.class.getName() + "Custom").newInstance();
		} catch(ClassNotFoundException t) {
			logger.debug("no class "+TrackingService.class.getName()+"Custom"+" found, will use default implementation for tracking");
			return null;
		} catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
