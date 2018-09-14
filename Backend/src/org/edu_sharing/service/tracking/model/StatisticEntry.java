package org.edu_sharing.service.tracking.model;

import org.edu_sharing.service.tracking.TrackingService;

import java.util.HashMap;
import java.util.Map;

public class StatisticEntry {
    private Map<TrackingService.EventType,Integer> counts=new HashMap<>();

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }
}
