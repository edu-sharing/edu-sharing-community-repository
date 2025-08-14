package org.edu_sharing.service.share;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface ShareInfoOplog {
    @JsonProperty(required = true)
    Long getId();
    @JsonProperty(required = true)
    Long getShareId();
    @JsonProperty(required = true)
    OpLogAction getAction();
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    java.util.Date getTimestamp();
}
