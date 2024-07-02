package org.edu_sharing.service.guest;

import lombok.Value;
import org.edu_sharing.lightbend.ConfigParam;

@Value
public class GuestConfigOption implements ConfigParam {
    String contextId;
}
