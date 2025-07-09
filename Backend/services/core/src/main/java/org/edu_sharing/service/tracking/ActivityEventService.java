package org.edu_sharing.service.tracking;

import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityEventService {

    private final ApplicationEventPublisher eventPublisher;

    public void trackActivityOnUser(String authorityName, UserActivityEventType type) {
        eventPublisher.publishEvent(new UserActivityEvent(authorityName, type));
    }


    public void trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, ActivityOnNodeEventType type, String authorityName) {
        eventPublisher.publishEvent(new ActivityOnNodeEvent(nodeRef, details, type, authorityName));
    }
}

