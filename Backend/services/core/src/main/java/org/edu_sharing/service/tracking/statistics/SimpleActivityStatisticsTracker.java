package org.edu_sharing.service.tracking.statistics;

import lombok.RequiredArgsConstructor;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.tracking.ActivityOnNodeEvent;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles the tracking of specific activities on nodes.
 * This component listens for events related to node activities and updates
 * the corresponding tracking properties on the node. It supports activities
 * such as downloading, viewing, and embedded viewing of materials.
 *
 * The handler ensures that changes to the tracking properties are applied
 * within a transactional context and updates the repository cache accordingly
 * to maintain consistency.
 *
 * This component is conditionally loaded based on the property
 * `repository.tracking.alfresco`. By default, it is enabled if the property
 * is either set to true or not defined.
 *
 * Annotations:
 * - {@code @Component}: Marks this class as a Spring Component.
 * - {@code @ConditionalOnProperty}: Configures conditional bean creation based on a property.
 * - {@code @RequiredArgsConstructor}: Generates a constructor with required arguments for
 *   final fields.
 *
 * Functionality:
 * - Listens to {@code ActivityOnNodeEvent} and handles events asynchronously.
 * - Updates the relevant tracking property for the node when a specified event occurs.
 * - Fetches and increments the property value, performs transactional updates via
 *   {@code RetryingTransactionHelper}, and manages cache synchronization.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "repository.tracking.alfresco", havingValue = "true", matchIfMissing = true)
public class SimpleActivityStatisticsTracker {

    private final NodeService nodeService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final BehaviourFilter policyBehaviourFilter;
    private final RepositoryCache repositoryCache;

    private final Map<ActivityOnNodeEventType, String> propertyEventMap = Map.of(
            ActivityOnNodeEventType.DOWNLOAD_MATERIAL, CCConstants.CCM_PROP_TRACKING_DOWNLOADS,
            ActivityOnNodeEventType.VIEW_MATERIAL, CCConstants.CCM_PROP_TRACKING_VIEWS,
            ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED, CCConstants.CCM_PROP_TRACKING_VIEWS
    );

    @Async
    @RunAsSystem
    @EventListener
    public void handleActivityOnNodeEvent(ActivityOnNodeEvent event) {
        String propertyName = propertyEventMap.get(event.getType());
        if (propertyName == null) {
            return;
        }

        NodeRef nodeRef = event.getNodeRef();
        String propertyValue = nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), propertyName);
        if (StringUtils.isBlank(propertyValue)) {
            propertyValue = "0";
        }

        final String propertyValueLong = Long.toString(Long.parseLong(propertyValue) + 1);


        retryingTransactionHelper.doInTransaction(() -> {

            policyBehaviourFilter.disableBehaviour(nodeRef);
            nodeService.setProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), propertyName, propertyValueLong, false);
            policyBehaviourFilter.enableBehaviour(nodeRef);

            // change the value in cache
            Map<String, Object> cache = repositoryCache.get(nodeRef.getId());
            if (cache != null) {
                cache.put(propertyName, propertyValueLong);
                repositoryCache.put(nodeRef.getId(), cache);
            }
            return null;
        });

    }
}
