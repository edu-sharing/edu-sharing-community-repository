package org.edu_sharing.service.tracking;

import org.apache.http.annotation.Obsolete;
import org.apache.log4j.Logger;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TrackingServiceFactory {
    private static Logger logger = Logger.getLogger(TrackingServiceFactory.class);

    private final TrackingServiceCustomInterface trackingServiceCustomInterface;

    public TrackingServiceFactory(Optional<TrackingServiceCustomInterface> trackingServiceCustomInterface) {
        this.trackingServiceCustomInterface = trackingServiceCustomInterface.orElseGet(this::getTrackingServiceCustomInterfaceByClassName);

    }


    public static TrackingService getTrackingService() {
        return (TrackingService) ApplicationContextFactory.getApplicationContext().getBean("trackingService");
    }

    public TrackingServiceCustomInterface getTrackingServiceCustom() {
        return trackingServiceCustomInterface;
    }

    @Obsolete
    @Nullable private TrackingServiceCustomInterface getTrackingServiceCustomInterfaceByClassName() {
        try {
            return (TrackingServiceCustomInterface) Class.forName(TrackingService.class.getName() + "Custom").newInstance();
        } catch (ClassNotFoundException t) {
            logger.debug("no class " + TrackingService.class.getName() + "Custom" + " found, will use default implementation for tracking");
            return null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
