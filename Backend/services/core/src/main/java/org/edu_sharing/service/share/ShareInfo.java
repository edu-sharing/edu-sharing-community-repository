package org.edu_sharing.service.share;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public interface ShareInfo {
    @JsonProperty(required = true)
    Long getId();
    @JsonProperty(required = true)
    String getNodeId();
    @JsonProperty(required = true)
    String getSharedBy();
    @JsonProperty(required = true)
    String getSharedWith();
    @JsonProperty(required = true)
    ShareStatus getShareStatus();
    @JsonProperty(required = true)
    ShareType getShareType();
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    Date getTimestamp();
}
