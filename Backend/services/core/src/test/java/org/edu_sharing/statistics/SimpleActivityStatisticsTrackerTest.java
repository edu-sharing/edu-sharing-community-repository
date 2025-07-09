package org.edu_sharing.statistics;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.tracking.ActivityOnNodeEvent;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.service.tracking.statistics.SimpleActivityStatisticsTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleActivityStatisticsTrackerTest {

    @Mock
    private NodeService nodeService;

    @Mock
    private RetryingTransactionHelper retryingTransactionHelper;

    @Mock
    private BehaviourFilter policyBehaviourFilter;

    @Mock
    private RepositoryCache repositoryCache;

    @InjectMocks
    private SimpleActivityStatisticsTracker underTest;


    @Test
    void handleActivityOnNodeEvent_ValidEvent_UpdatesPropertyAndCache() {
        // Arrange
        NodeRef nodeRef = new NodeRef("protocol", "storeIdentifier", "id");
        ActivityOnNodeEvent event = new ActivityOnNodeEvent(nodeRef, null, ActivityOnNodeEventType.DOWNLOAD_MATERIAL, "authorityName");
        Map<String, Object> cache = mock();

        when(nodeService.getProperty(eq(nodeRef.getStoreRef().getProtocol()), eq(nodeRef.getStoreRef().getIdentifier()), eq(nodeRef.getId()), eq(CCConstants.CCM_PROP_TRACKING_DOWNLOADS)))
                .thenReturn("5");

        doAnswer(invocation -> {
            ((RetryingTransactionHelper.RetryingTransactionCallback<?>) invocation.getArgument(0)).execute();
            return null;
        }).when(retryingTransactionHelper).doInTransaction(any());


        when(repositoryCache.get(nodeRef.getId())).thenReturn(cache);

        // Act
        underTest.handleActivityOnNodeEvent(event);

        // Assert
        verify(nodeService).setProperty(eq(nodeRef.getStoreRef().getProtocol()), eq(nodeRef.getStoreRef().getIdentifier()), eq(nodeRef.getId()), eq(CCConstants.CCM_PROP_TRACKING_DOWNLOADS), eq("6"), eq(false));
        verify(policyBehaviourFilter).disableBehaviour(nodeRef);
        verify(policyBehaviourFilter).enableBehaviour(nodeRef);
        verify(cache).put(CCConstants.CCM_PROP_TRACKING_DOWNLOADS, "6");
        verify(repositoryCache).put(nodeRef.getId(), cache);
    }

    @Test
    void handleActivityOnNodeEvent_BlankPropertyValue_InitializesToOne() {
        // Arrange
        NodeRef nodeRef = new NodeRef("protocol", "storeIdentifier", "id");
        ActivityOnNodeEvent event = new ActivityOnNodeEvent(nodeRef, null, ActivityOnNodeEventType.VIEW_MATERIAL, "authorityName");

        when(nodeService.getProperty(eq(nodeRef.getStoreRef().getProtocol()), eq(nodeRef.getStoreRef().getIdentifier()), eq(nodeRef.getId()), eq(CCConstants.CCM_PROP_TRACKING_VIEWS)))
                .thenReturn("");

        doAnswer(invocation -> {
            ((RetryingTransactionHelper.RetryingTransactionCallback<?>) invocation.getArgument(0)).execute();
            return null;
        }).when(retryingTransactionHelper).doInTransaction(any());

        // Act
        underTest.handleActivityOnNodeEvent(event);

        // Assert
        verify(nodeService).setProperty(eq(nodeRef.getStoreRef().getProtocol()), eq(nodeRef.getStoreRef().getIdentifier()), eq(nodeRef.getId()), eq(CCConstants.CCM_PROP_TRACKING_VIEWS), eq("1"), eq(false));
        verify(policyBehaviourFilter).disableBehaviour(nodeRef);
        verify(policyBehaviourFilter).enableBehaviour(nodeRef);
    }
}
