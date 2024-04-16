package org.edu_sharing.restservices.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.service.tracking.TrackingService;

import java.util.HashMap;
import java.util.Map;

public interface NodeStatsEntry {
    void setStats(NodeStats stats);

    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeStats {
        @JsonProperty
        @Getter
        @Setter
        Map<TrackingService.EventType, Integer> total;

    }
}
