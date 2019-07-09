package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.Map;

public class TrackingNode extends Tracking {
    private final Node node;
    public TrackingNode(Node node, String date, Map<TrackingService.EventType,Integer> counts,Map<String,String> data){
        super(date,counts,data);
        this.node=node;
    }

    public Node getNode() {
        return node;
    }
}
