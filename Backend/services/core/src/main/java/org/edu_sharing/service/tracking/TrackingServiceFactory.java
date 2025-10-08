package org.edu_sharing.service.tracking;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.annotation.Obsolete;
import org.apache.log4j.Logger;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@Slf4j
@Component
public class TrackingServiceFactory {
    private final TrackingServiceCustomInterface trackingServiceCustomInterface;

    public TrackingServiceFactory(Optional<TrackingServiceCustomInterface> trackingServiceCustomInterface) {
        this.trackingServiceCustomInterface = trackingServiceCustomInterface.orElseGet(this::getTrackingServiceCustomInterfaceByClassName);

    }


    public static ActivityStatisticService getTrackingService() {
        return ApplicationContextFactory.getApplicationContext().getBean(ActivityStatisticService.class);
    }

    public TrackingServiceCustomInterface getTrackingServiceCustom() {
        return trackingServiceCustomInterface;
    }

    @Obsolete
    @Nullable private TrackingServiceCustomInterface getTrackingServiceCustomInterfaceByClassName() {
        try {
            TrackingServiceCustomInterface trackingServiceCustomInterface = (TrackingServiceCustomInterface) Class.forName(ActivityStatisticService.class.getName() + "Custom").getDeclaredConstructor().newInstance();
            log.warn("Instantiating TrackingServiceCustomInterface by class name is obsolete. Please use Spring beans instead (e.g. @Service annotation)");
            return trackingServiceCustomInterface;
        } catch (ClassNotFoundException t) {
            log.debug("no class " + ActivityStatisticService.class.getName() + "Custom" + " found, will use default implementation for tracking");
            return null;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
