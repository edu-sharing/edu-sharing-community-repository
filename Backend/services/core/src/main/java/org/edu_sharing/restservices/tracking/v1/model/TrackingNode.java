package org.edu_sharing.restservices.tracking.v1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.tracking.ActivityStatisticService;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrackingNode extends Tracking {
    private final Node node;

    public TrackingNode(Node node, TrackingAuthority trackingAuthority, String date, Map<String,Integer> counts, Map<String,Serializable> fields, Map<String, Map<String, Map<String, Long>>> groups){
        super(date, trackingAuthority,counts,fields,groups);
        this.node=node;
    }
}
