package org.edu_sharing.service.tracking.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatisticEntryNode extends StatisticEntry {
    private String node;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StatisticEntryNode) {
            if (node != null)
                return node.equals(((StatisticEntryNode) obj).node);
            if (date != null)
                return date.equals(((StatisticEntryNode) obj).date);
        }
        return super.equals(obj);
    }
}
