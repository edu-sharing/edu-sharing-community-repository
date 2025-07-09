package org.edu_sharing.service.tracking;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;

@Data
@AllArgsConstructor
public class ActivityOnNodeEvent {
    NodeRef nodeRef;
    NodeTrackingDetails details;
    ActivityOnNodeEventType type;
    String authorityName;
}
