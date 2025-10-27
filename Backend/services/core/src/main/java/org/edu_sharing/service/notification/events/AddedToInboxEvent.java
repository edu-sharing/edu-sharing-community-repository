package org.edu_sharing.service.notification.events;

import java.util.List;
import java.util.Map;

public record AddedToInboxEvent(
        String senderAuthority,
        String receiverAuthority,
        String nodeId,
        String nodeType,
        List<String> aspects,
        Map<String, Object> properties,
        String comment) {
}
