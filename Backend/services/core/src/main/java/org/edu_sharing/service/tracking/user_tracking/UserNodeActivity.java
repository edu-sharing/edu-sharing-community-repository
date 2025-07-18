package org.edu_sharing.service.tracking.user_tracking;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public interface UserNodeActivity {
    @JsonProperty(required = true)
    String getId();
    @JsonProperty(required = true)
    String getNodeId();
    @JsonProperty(required = true)
    String getUsername();
    @JsonProperty(required = true)
    String getType();
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    Date getTimestamp();
}
