package org.edu_sharing.service.notification.events;

import org.edu_sharing.alfresco.service.config.model.ConfigRating;
import org.edu_sharing.service.notification.Status;
import org.edu_sharing.service.rating.RatingDetails;

import java.util.List;
import java.util.Map;

public record RatingChangedEvent(
        String nodeId,
        String nodeType,
        List<String> aspects,
        Map<String, Object> nodeProperties,
        ConfigRating.RatingMode ratingMode,
        Double rating,
        RatingDetails accumulatedRatings,
        Status removed
) {
}
