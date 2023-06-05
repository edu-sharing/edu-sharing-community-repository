package org.edu_sharing.service.notification;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class NotificationConfig implements Serializable {
    @Getter
    @Setter
    public static class NotificationConfigIntervals implements Serializable {
        private NotificationConfigInterval addToCollectionEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval commentEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval inviteEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval nodeIssueEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval ratingEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval workflowEvent = NotificationConfigInterval.immediately;
        private NotificationConfigInterval metadataSuggestionEvent = NotificationConfigInterval.immediately;
    }
    enum NotificationConfigMode {
        uniformly,
        individual
    }
    enum NotificationConfigInterval {
        immediately,
        disabled,
        daily,
        weekly,
    }
    // if mode == uniformly
    private NotificationConfigMode configMode;
    private NotificationConfigInterval defaultInterval = NotificationConfigInterval.immediately;
    // if mode == individual
    private NotificationConfigIntervals intervals = new NotificationConfigIntervals();
}
