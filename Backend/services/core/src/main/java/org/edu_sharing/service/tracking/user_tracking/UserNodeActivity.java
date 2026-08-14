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
    /**
     * A write/polling cursor value (e.g. the underlying store's write time), not necessarily when
     * the activity actually happened - see {@link #getOccurredAt()} for that.
     */
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getTimestamp();
    /**
     * When the activity actually happened. Use this, not {@link #getTimestamp()}, for anything
     * shown to users.
     */
    @JsonProperty(required = true)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date getOccurredAt();
}
