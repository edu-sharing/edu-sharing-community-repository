package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.tracking.TrackingService;

import java.util.Map;

public class Tracking {
    private final Map<TrackingService.EventType, Integer> counts;
    private final String date;
    private final Map<String, String> data;

    public Tracking(String date, Map<TrackingService.EventType,Integer> counts, Map<String,String> data){
        this.date=date;
        this.counts=counts;
        this.data=data;
    }

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }

    public String getDate() {
        return date;
    }

    public Map<String, String> getData() {
        return data;
    }
}
