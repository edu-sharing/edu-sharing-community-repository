package org.edu_sharing.service.notification;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationConfig implements Serializable {
    @Data
    public static class NotificationIntervals implements Serializable {
        private NotificationInterval addToCollectionEvent = NotificationInterval.immediately;
        private NotificationInterval commentEvent = NotificationInterval.immediately;
        private NotificationInterval inviteEvent = NotificationInterval.immediately;
        private NotificationInterval nodeIssueEvent = NotificationInterval.immediately;
        private NotificationInterval ratingEvent = NotificationInterval.immediately;
        private NotificationInterval workflowEvent = NotificationInterval.immediately;
        private NotificationInterval metadataSuggestionEvent = NotificationInterval.immediately;
    }

    public enum NotificationConfigMode {
        uniformly,
        individual
    }

    public enum NotificationInterval {
        immediately,
        disabled,
        daily,
        weekly,
    }

    // if mode == uniformly
    private NotificationConfigMode configMode;
    private NotificationInterval defaultInterval = NotificationInterval.immediately;
    // if mode == individual
    private NotificationIntervals intervals = new NotificationIntervals();

    public NotificationInterval getAddToCollectionEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getAddToCollectionEvent()
                : defaultInterval;
    }

    public NotificationInterval getCommentEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getCommentEvent()
                : defaultInterval;
    }

    public NotificationInterval getInviteEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getInviteEvent()
                : defaultInterval;
    }

    public NotificationInterval getNodeIssueEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getNodeIssueEvent()
                : defaultInterval;
    }

    public NotificationInterval getRatingEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getRatingEvent()
                : defaultInterval;
    }

    public NotificationInterval getWorkflowEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getWorkflowEvent()
                : defaultInterval;
    }

    public NotificationInterval getMetadataSuggestionEvent() {
        return configMode == NotificationConfigMode.individual
        ? intervals.getMetadataSuggestionEvent()
        : defaultInterval;
    }
}
