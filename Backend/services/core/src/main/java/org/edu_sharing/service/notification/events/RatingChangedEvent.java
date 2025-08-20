package org.edu_sharing.service.notification.events;

import org.edu_sharing.service.notification.Status;
import org.edu_sharing.service.rating.RatingDetails;

import java.util.List;
import java.util.Map;

public record RatingChangedEvent(
        String nodeId,
        String nodeType,
        List<String> aspects,
        Map<String, Object> nodeProperties,
        Double rating,
        RatingDetails accumulatedRatings,
        Status removed
) {
}
