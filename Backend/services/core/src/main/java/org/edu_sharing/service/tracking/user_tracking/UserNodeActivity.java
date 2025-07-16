package org.edu_sharing.service.tracking.user_tracking;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public interface UserNodeActivity {
    String getNodeId();
    String getUserId();
    String getType();
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    Date   getTimestamp();
}
