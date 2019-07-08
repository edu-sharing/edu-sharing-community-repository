package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.Map;

public class TrackingNode {
    private final Node node;
    private final Map<TrackingService.EventType, Integer> counts;
    private final String date;
    private final Map<String, String> data;

    public TrackingNode(Node node, String date, Map<TrackingService.EventType,Integer> counts,Map<String,String> data){
        this.node=node;
        this.date=date;
        this.counts=counts;
        this.data=data;
    }

    public Node getNode() {
        return node;
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
