package org.edu_sharing.service.notification;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NotificationConfig {
    enum NotificationConfigMode {
        uniformly,
        individual
    }
    enum NotificationConfigInterval {
        disabled,
        daily,
        weekly,
        immediately,
    }
    // if mode == uniformly
    private NotificationConfigMode configMode;
    private NotificationConfigInterval defaultInterval;
    // if mode == individual
    private Map<NotificationEventEnum, NotificationConfigInterval> intervals;


}
