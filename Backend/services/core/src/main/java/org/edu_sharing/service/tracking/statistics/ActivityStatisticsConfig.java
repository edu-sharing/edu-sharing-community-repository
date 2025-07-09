package org.edu_sharing.service.tracking.statistics;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.service.tracking.UserTrackingMode;

@Data
@ConfigurationProperties(prefix = "repository.tracking")
public class ActivityStatisticsConfig {
    private boolean sharedWithMediacenter;
    @Optional
    private UserTrackingMode userMode;
}
