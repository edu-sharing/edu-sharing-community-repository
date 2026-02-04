package org.edu_sharing.restservices.relation.v1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty(required = true)
    Node fromNode;
    @JsonProperty(required = true)
    Node toNode;
    @JsonProperty(required = true)
    User createdBy;
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date createdAt;
    User modifiedBy;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date modifiedAt;
    @JsonProperty(required = true)
    OutputRelationType type;
    @JsonProperty(required = true)
    OutputRelationType reverseType;
    @JsonProperty(required = true)
    boolean isAiGenerated;
    @JsonProperty(required = true)
    NodeRelationDataEvaluation evaluation;
    @JsonProperty(required = true)
    Map<String, Object> metadata;
}

