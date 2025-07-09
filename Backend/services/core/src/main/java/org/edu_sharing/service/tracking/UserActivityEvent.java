package org.edu_sharing.service.tracking;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserActivityEvent {
    String authorityName;
    UserActivityEventType type;
}
