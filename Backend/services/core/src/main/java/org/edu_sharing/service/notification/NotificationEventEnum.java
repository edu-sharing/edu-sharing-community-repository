package org.edu_sharing.service.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;

public enum NotificationEventEnum {
    AddToCollectionEvent,
    CommentEvent,
    InviteEvent,
    NodeIssueEvent,
    RatingEvent,
    WorkflowEvent,
    MetadataSuggestionEvent,
}
