package org.edu_sharing.service.notification.events;

import org.edu_sharing.service.notification.Status;

import java.util.List;
import java.util.Map;

public record AddCollectionEvent(
        String collectionId,
        String refNodeId,
        String collectionType,
        List<String> collectionAspects,
        Map<String, Object> collectionProperties,
        String nodeType,
        List<String> nodeAspects,
        Map<String, Object> nodeProperties,
        Status status
) {
}
