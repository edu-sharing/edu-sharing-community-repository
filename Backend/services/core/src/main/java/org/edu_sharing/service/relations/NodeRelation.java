package org.edu_sharing.service.relations;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class NodeRelation {
    public String node;
    public List<RelationData> relations = new ArrayList<>();

    public NodeRelation(String node) {
        this.node = node;
    }
}

