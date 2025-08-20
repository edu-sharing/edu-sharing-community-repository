package org.edu_sharing.service.notification.events;

import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;

import java.util.List;
import java.util.Map;

public record MetadataSetSuggestionEvent(
        MdsValue mdsValue,
        MetadataWidget widgetDefinition,
        List<String> nodeIds,
        List<String> nodeTypes,
        List<List<String>> aspects,
        List<Map<String, Object>> nodePropertiesList
) {
}
