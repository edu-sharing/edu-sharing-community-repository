package org.edu_sharing.service.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEventService {

    private final ApplicationEventPublisher eventPublisher;

    public void trackActivityOnUser(String authorityName, UserActivityEventType type) {
        try {
            eventPublisher.publishEvent(new UserActivityEvent(authorityName, type));
        } catch (Exception e) {
            log.error("Error while tracking activity on user", e);
        }
    }


    public void trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, ActivityOnNodeEventType type, String authorityName) {
        try {
            eventPublisher.publishEvent(new ActivityOnNodeEvent(nodeRef, details, type, authorityName));
        } catch (Exception e) {
            log.error("Error while tracking activity on node", e);
        }
    }
}

