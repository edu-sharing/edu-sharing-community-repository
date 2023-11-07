package org.edu_sharing.service.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationConfig implements Serializable {
    @Data
    public static class NotificationIntervals implements Serializable {
        private NotificationInterval addToCollectionEvent = NotificationInterval.immediately;
        private NotificationInterval proposeForCollectionEvent = NotificationInterval.immediately;
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
    @JsonProperty
    private NotificationConfigMode configMode = NotificationConfigMode.uniformly;
    @JsonProperty
    private NotificationInterval defaultInterval = NotificationInterval.immediately;
    @JsonProperty
    // if mode == individual
    private NotificationIntervals intervals = new NotificationIntervals();

    @JsonIgnore
    public NotificationInterval getAddToCollectionEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getAddToCollectionEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getProposeForCollectionEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getAddToCollectionEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getCommentEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getCommentEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getInviteEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getInviteEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getNodeIssueEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getNodeIssueEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getRatingEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getRatingEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getWorkflowEvent() {
        return configMode == NotificationConfigMode.individual
                ? intervals.getWorkflowEvent()
                : defaultInterval;
    }

    @JsonIgnore
    public NotificationInterval getMetadataSuggestionEvent() {
        return configMode == NotificationConfigMode.individual
        ? intervals.getMetadataSuggestionEvent()
        : defaultInterval;
    }
}
