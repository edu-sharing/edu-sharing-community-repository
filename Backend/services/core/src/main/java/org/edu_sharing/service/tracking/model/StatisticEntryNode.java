package org.edu_sharing.service.tracking.model;

import org.edu_sharing.service.tracking.TrackingService;

import java.util.HashMap;
import java.util.Map;

public class StatisticEntryNode extends StatisticEntry{
    private String node;

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StatisticEntryNode){
            if(node!=null)
                return node.equals(((StatisticEntryNode) obj).node);
            if(date!=null)
                return date.equals(((StatisticEntryNode) obj).date);
        }
        return super.equals(obj);
    }
}
