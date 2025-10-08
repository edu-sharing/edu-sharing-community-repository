package org.edu_sharing.service.tracking.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.tracking.*;
import org.edu_sharing.service.tracking.ibatis.NodeTrackingMapper;
import org.edu_sharing.service.tracking.ibatis.NodeTrackingEntry;
import org.edu_sharing.service.tracking.ibatis.UserTrackingEntry;
import org.edu_sharing.service.tracking.ibatis.UserTrackingMapper;
import org.json.JSONObject;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdvancedActivityStatisticsTracker {

    private final NodeService nodeService;
    private final org.alfresco.service.cmr.repository.NodeService alfNodeService;
    private final MediacenterService mediacenterService;
    private final TrackingServiceFactory trackingServiceFactory;
    private final NodeTrackingMapper nodeTrackingMapper;
    private final UserTrackingMapper userTrackingMapper;
    private final GuestService guessService;
    private final ActivityStatisticsUtil activityStatisticsUtil;
    private final ActivityStatisticsConfig config;


    private final Set<ActivityOnNodeEventType> eventsToListenTo = new HashSet<>(Arrays.asList(
            ActivityOnNodeEventType.DOWNLOAD_MATERIAL,
            ActivityOnNodeEventType.OPEN_EXTERNAL_LINK,
            ActivityOnNodeEventType.VIEW_MATERIAL,
            ActivityOnNodeEventType.VIEW_COLLECTION,
            ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED
    ));

    @Async
    @RunAsSystem
    @EventListener
    public void handleActivityOnNodeEvent(ActivityOnNodeEvent event) {
        if(!eventsToListenTo.contains(event.getType())) {
            return;
        }

        String nodeVersion = Optional.ofNullable(event.getDetails())
                .map(NodeTrackingDetails::getNodeVersion)
                .orElse(null);

        Map<QName, Serializable> nativeProps = alfNodeService.getProperties(event.getNodeRef());


        String version = nodeVersion;
        if (StringUtils.isBlank(nodeVersion) || nodeVersion.equals("-1")) {
            version = nodeService.getProperty(event.getNodeRef().getStoreRef().getProtocol(), event.getNodeRef().getStoreRef().getIdentifier(), event.getNodeRef().getId(), CCConstants.CM_PROP_VERSIONABLELABEL);
        }
        String[] mediacenters = null;
        if (config.isSharedWithMediacenter()) {
            try {
                mediacenters = mediacenterService.getMediacenterAuthoritiesByNode(event.getNodeRef().getId()).toArray(String[]::new);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        String originalNodeRef = getOriginalNodeRef(event, nativeProps);

        nodeTrackingMapper.insertNode(
                new NodeTrackingEntry(
                        (Long) nativeProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_DBID)),
                        event.getNodeRef().getId(),
                        originalNodeRef,
                        version,
                        activityStatisticsUtil.getTrackedUsername(event.getAuthorityName()),
                        activityStatisticsUtil.getAuthorityOrganizations(),
                        activityStatisticsUtil.getAuthorityMediacenters(),
                        new Date(),
                        event.getType(),
                        buildJson(event),
                        nodeService.getProperty(event.getNodeRef().getStoreRef().getProtocol(), event.getNodeRef().getStoreRef().getIdentifier(), event.getNodeRef().getId(), CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
                        mediacenters)
        );

    }

    @Async
    @EventListener
    public void handleUserActivityEvent(UserActivityEvent event) {
        if (event.getAuthorityName() == null
                || guessService.getAllGuestAuthorities().contains(event.getAuthorityName())
                || event.getAuthorityName().equals(AuthenticationUtil.getSystemUserName())
        ) {
            return;
        }

        AuthenticationUtil.runAs(() -> {
                    userTrackingMapper.insertNode(
                            new UserTrackingEntry(
                                    activityStatisticsUtil.getTrackedUsername(event.getAuthorityName()),
                                    activityStatisticsUtil.getAuthorityOrganizations(),
                                    activityStatisticsUtil.getAuthorityMediacenters(),
                                    new Date(),
                                    event.getType(),
                                    buildJson(event)
                            )
                    );
                    return null;
                },
                event.getAuthorityName()
        );

    }

    private String getOriginalNodeRef(ActivityOnNodeEvent event, Map<QName, Serializable> nativeProps) {
        String originalNodeRef = event.getNodeRef().getId();
        try {
            if (nodeService.hasAspect(event.getNodeRef().getStoreRef().getProtocol(), event.getNodeRef().getStoreRef().getIdentifier(), event.getNodeRef().getId(), CCConstants.CCM_ASPECT_PUBLISHED)) {
                originalNodeRef = ((NodeRef) nativeProps.get(QName.createQName(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL))).getId();
            } else if (nodeService.hasAspect(event.getNodeRef().getStoreRef().getProtocol(), event.getNodeRef().getStoreRef().getIdentifier(), event.getNodeRef().getId(), CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
                originalNodeRef = nodeService.getProperty(event.getNodeRef().getStoreRef().getProtocol(), event.getNodeRef().getStoreRef().getIdentifier(), event.getNodeRef().getId(), CCConstants.CCM_PROP_IO_ORIGINAL);
            }
        } catch (Throwable ignored) {
        }
        return originalNodeRef;
    }

    protected JSONObject buildJson(ActivityOnNodeEvent event) {
        TrackingServiceCustomInterface trackingServiceCustom = trackingServiceFactory.getTrackingServiceCustom();
        if (trackingServiceCustom != null) {
            return trackingServiceCustom.buildJson(event);
        }
        return null;
    }

    protected JSONObject buildJson(UserActivityEvent event) {
        TrackingServiceCustomInterface trackingServiceCustom = trackingServiceFactory.getTrackingServiceCustom();
        if (trackingServiceCustom != null) {
            return trackingServiceCustom.buildJson(event);
        }
        return null;
    }
}
