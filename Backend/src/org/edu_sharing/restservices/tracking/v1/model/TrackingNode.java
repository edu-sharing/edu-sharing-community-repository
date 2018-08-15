package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.Map;

public class TrackingNode {
    private final Node node;
    private final Map<TrackingService.EventType, Integer> counts;

    public TrackingNode(Node node, Map<TrackingService.EventType,Integer> counts){
        this.node=node;
        this.counts=counts;
    }

    public Node getNode() {
        return node;
    }

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }
}
