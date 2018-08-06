package org.edu_sharing.service.tracking;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.DownloadServlet;

public class TrackingServiceFactory {
    private static Logger logger = Logger.getLogger(TrackingServiceFactory.class);

    public static TrackingService getTrackingService() {
		try {
			return (TrackingService) Class.forName(TrackingService.class.getName()+"JSONImpl").newInstance();
		}catch(Throwable t) {
		    logger.info("no custom tracking service found, will use default implementation");
			//throw new RuntimeException(t);
            return new TrackingServiceDefault();

        }
	}
}
