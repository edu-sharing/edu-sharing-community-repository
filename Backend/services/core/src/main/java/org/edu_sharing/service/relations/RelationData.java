package org.edu_sharing.service.relations;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;

public interface RelationData {
    @JsonProperty(required = true)
    String getFromNode();
    @JsonProperty(required = true)
    String getToNode();
    @JsonProperty(required = true)
    String getCreatedBy();
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getCreatedAt();
    String getModifiedBy();
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getModifiedAt();
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getTimestamp();
    @JsonProperty(required = true)
    OutputRelationType getType();
    @JsonProperty(required = true)
    OutputRelationType getReverseType();

    @JsonProperty(required = true)
    boolean isAiGenerated();
    Evaluation getEvaluation();
    Map<String, Object> getMetadata();
}

