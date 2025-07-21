package org.edu_sharing.service.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;

import java.util.Date;

@AllArgsConstructor
@Data
public class SearchUserEvent {
    NodeRef nodeRef;
    String initiator;
    Date timestamp;
    ActivityOnNodeEventType eventType;
}
