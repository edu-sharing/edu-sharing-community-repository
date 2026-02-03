package org.edu_sharing.restservices.relation.v1.model;

import lombok.Builder;
import lombok.Value;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.User;
import org.edu_sharing.service.relations.Evaluation;
import org.edu_sharing.service.relations.OutputRelationType;

import java.util.Date;
import java.util.Map;

@Value
@Builder
public class NodeRelationData {
    Node fromNode;
    Node toNode;
    User creator;
    Date timestamp;
    OutputRelationType type;
    OutputRelationType reverseType;
    boolean isAiGenerated;
    Evaluation evaluation;
    Map<String, Object> metadata;
}
