package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.lang.time.DateUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveNodeVersionsJobTest {

    private RemoveNodeVersionsJob underTest;

    public String v1Label = "v1";
    public String v2Label = "v2";
    public String v3Label = "v3";
    public String v4Label = "v4";
    public String v5Label = "v5";
    public String v6Label = "v6";
    private NodeRef node = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "current");
    private NodeRef nodeV1 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v1");
    private NodeRef nodeV2 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v2");
    private NodeRef nodeV3 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v3");
    private NodeRef nodeV4 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v4");
    private NodeRef nodeV5 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v5");
    private NodeRef nodeV6 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "v6");

    @Mock
    private Usage2Service usage2Service;

    @Mock
    private VersionService versionService;

    @Mock
    private NodeService nodeService;

    @Mock
    Version v1, v2, v3, v4, v5, v6;


    @Captor
    ArgumentCaptor<Version> versionArgumentCaptor;

    @Captor
    ArgumentCaptor<NodeRef> nodeArgumentCaptor;


    @BeforeEach
    void setUp() {
        underTest = new RemoveNodeVersionsJob();
        underTest.setVersionService(versionService);
        underTest.setUsage2Service(usage2Service);
        underTest.setNodeService(nodeService);

        lenient().when(v1.getVersionLabel()).thenReturn(v1Label);
        lenient().when(v2.getVersionLabel()).thenReturn(v2Label);
        lenient().when(v3.getVersionLabel()).thenReturn(v3Label);
        lenient().when(v4.getVersionLabel()).thenReturn(v4Label);
        lenient().when(v5.getVersionLabel()).thenReturn(v5Label);
        lenient().when(v6.getVersionLabel()).thenReturn(v6Label);

        lenient().when(v1.getVersionedNodeRef()).thenReturn(nodeV1);
        lenient().when(v2.getVersionedNodeRef()).thenReturn(nodeV2);
        lenient().when(v3.getVersionedNodeRef()).thenReturn(nodeV3);
        lenient().when(v4.getVersionedNodeRef()).thenReturn(nodeV4);
        lenient().when(v5.getVersionedNodeRef()).thenReturn(nodeV5);
        lenient().when(v6.getVersionedNodeRef()).thenReturn(nodeV6);

        Date now = new Date();
        lenient().when(v1.getFrozenModifiedDate()).thenReturn(now);
        lenient().when(v2.getFrozenModifiedDate()).thenReturn(DateUtils.addDays(now, -1));
        lenient().when(v3.getFrozenModifiedDate()).thenReturn(DateUtils.addDays(now, -2));
        lenient().when(v4.getFrozenModifiedDate()).thenReturn(DateUtils.addDays(now, -3));
        lenient().when(v5.getFrozenModifiedDate()).thenReturn(DateUtils.addDays(now, -4));
        lenient().when(v6.getFrozenModifiedDate()).thenReturn(DateUtils.addDays(now, -5));
    }

    @Test
    void DeleteVersionsThatAreOlderThan1DayButKeepThe2LatestAndTheCurrentVersionAndThatHaveUsages() throws Exception {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3, v4, v5, v6);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(v5Label);

        Usage usage=mock(Usage.class);
        when(usage.getUsageVersion()).thenReturn(v4Label);
        List<Usage> usages = Collections.singletonList(usage);

        when(usage2Service.getUsages(anyString(), eq(node.getId()), isNull(), isNull())).thenReturn(usages);

        underTest.setKeepAtLeast(2);
        underTest.setOlderThan("P1D");
        underTest.handleNode(node);

        // then
        verify(versionService, times(2)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Arrays.asList(node, node), allNodes);
        assertEquals(Arrays.asList(v3, v6), allValues);
    }

    @Test
    void HasNoVersions() {
        // when
        when(versionService.getVersionHistory(node)).thenReturn(null);
        underTest.handleNode(node);

        // then
        verify(versionService, never()).deleteVersion(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void DeleteAllVersions() {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(null);

        underTest.handleNode(node);

        // then
        verify(versionService, times(3)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Arrays.asList(node, node, node), allNodes);
        assertEquals(Arrays.asList(v1, v2, v3), allValues);
    }

    @Test
    void KeepCurrentVersion() {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(v2Label);

        underTest.handleNode(node);

        // then
        verify(versionService, times(2)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Arrays.asList(node, node), allNodes);
        assertEquals(Arrays.asList(v1, v3), allValues);
    }

    @Test
    void KeepThe2LatestVersions() {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(null);

        underTest.setKeepAtLeast(2);
        underTest.handleNode(node);

        // then
        verify(versionService, times(1)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Collections.singletonList(node), allNodes);
        assertEquals(Collections.singletonList(v3), allValues);
    }

    @Test
    void KeepMoreVersionAsItContains() {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(null);

        underTest.setKeepAtLeast(4);
        underTest.handleNode(node);

        // then
        verify(versionService, never()).deleteVersion(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void DeleteVersionsThatAreOlderThen2Days() {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(null);

        underTest.setOlderThan("P2D");
        underTest.handleNode(node);

        // then
        verify(versionService, times(1)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Collections.singletonList(node), allNodes);
        assertEquals(Collections.singletonList(v3), allValues);
    }

    @Test
    void DeleteOnlyThoseThatHaveNoUsages() throws Exception {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);
        Collection<Version> versionCollection = Arrays.asList(v1, v2, v3);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionCollection);
        when(nodeService.getProperty(anyString(), anyString(), eq(node.getId()), eq(CCConstants.LOM_PROP_LIFECYCLE_VERSION)))
                .thenReturn(null);

        Usage usage=mock(Usage.class);
        when(usage.getUsageVersion()).thenReturn(v2Label);
        List<Usage> usages = Collections.singletonList(usage);

        when(usage2Service.getUsages(anyString(), eq(node.getId()), isNull(), isNull())).thenReturn(usages);

        underTest.handleNode(node);

        // then
        verify(versionService, times(2)).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());

        List<NodeRef> allNodes = nodeArgumentCaptor.getAllValues();
        List<Version> allValues = versionArgumentCaptor.getAllValues();

        assertEquals(Arrays.asList(node, node), allNodes);
        assertEquals(Arrays.asList(v1, v3), allValues);
    }

    @Test
    void GetUsagesThrowsException() throws Exception {
        // given
        VersionHistory versionHistory = mock(VersionHistory.class);

        // when
        when(versionService.getVersionHistory(node)).thenReturn(versionHistory);
        when(usage2Service.getUsages(anyString(), eq(node.getId()), isNull(), isNull())).thenThrow(Exception.class);

        underTest.handleNode(node);

        // then
        verify(versionService, never()).deleteVersion(nodeArgumentCaptor.capture(), versionArgumentCaptor.capture());
    }
}