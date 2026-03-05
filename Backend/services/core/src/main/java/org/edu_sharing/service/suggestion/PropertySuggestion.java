package org.edu_sharing.service.suggestion;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public interface PropertySuggestion {
    @JsonProperty(required = true)
    String getId();
    @JsonProperty(required = true)
    String getNodeId();
    @JsonProperty(required = true)
    String getVersion();

    @JsonProperty(required = true)
    String getPropertyId();
    @JsonProperty(required = true)
    Object getValue();

    @JsonProperty(required = true)
    SuggestionType getType();
    @JsonProperty(required = true)
    SuggestionStatus getStatus();
    @JsonProperty(required = true)
    String getDescription();
    double getConfidence();

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getCreated();
    String getCreatedBy();
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getModified();
    String getModifiedBy();

    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getTimestamp();

}
