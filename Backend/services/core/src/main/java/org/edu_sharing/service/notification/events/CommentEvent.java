package org.edu_sharing.service.notification.events;

import org.edu_sharing.service.notification.Status;

import java.util.List;
import java.util.Map;

public record CommentEvent(
        String nodeId,
        String comment,
        String commentReference,
        String nodeType,
        List<String> aspects,
        Map<String, Object> nodeProperties,
        Status status
) {
}
