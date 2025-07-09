package org.edu_sharing.restservices.shared;

import lombok.*;
import org.edu_sharing.service.tracking.ActivityStatisticService;

import java.util.Map;

public interface NodeStatsEntry {
    void setStats(NodeStats stats);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class NodeStats {
        Map<String, Integer> total;

    }
}
