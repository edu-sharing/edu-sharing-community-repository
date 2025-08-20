package org.edu_sharing.service.notification.events;

import org.edu_sharing.service.notification.NotificationService;

import java.util.List;
import java.util.Map;

public record NodeIssueEvent(
        String nodeId,
        NotificationService.NotifyMode mode,
        String reason,
        String nodeType,
        List<String> aspects,
        Map<String, Object> nodeProperties,
        String userEmail,
        String userComment
) {
}
