package org.edu_sharing.service.notification.events;

import java.util.List;
import java.util.Map;

public record WorkflowChangedEvent(
        String nodeId,
        String nodeType,
        List<String> aspects,
        Map<String, Object> nodeProperties,
        String receiver,
        String comment,
        String status
) {
}
