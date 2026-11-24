package org.edu_sharing.service.permission;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedConstruction;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTimedPermissionTest {
    private static final String NODE_ID = "node-1";
    private static final String AUTHORITY = "GROUP_MEDIA_CENTER_PROXY_DE-RP-MZ-25";
    private static final String PERMISSION = "Consumer";

    @Mock
    org.alfresco.service.cmr.security.PermissionService alfrescoPermissionService;

    @Mock
    TimedPermissionMapper timedPermissionMapper;

    PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        // the service creates a MCAlfrescoAPIClient on construction, which needs a home repository
        try (MockedConstruction<MCAlfrescoAPIClient> ignored = Mockito.mockConstruction(MCAlfrescoAPIClient.class)) {
            permissionService = Mockito.spy(new PermissionServiceImpl(
                    Optional.empty(),
                    Mockito.mock(NodeService.class),
                    Mockito.mock(PersonService.class),
                    Mockito.mock(ToolPermissionService.class),
                    Mockito.mock(org.edu_sharing.service.nodeservice.NodeService.class),
                    timedPermissionMapper,
                    Mockito.mock(OrganisationService.class),
                    Mockito.mock(AuthorityService.class),
                    Mockito.mock(BehaviourFilter.class),
                    Mockito.mock(GuestService.class),
                    alfrescoPermissionService,
                    Mockito.mock(RetryingTransactionHelper.class),
                    Mockito.mock(RepositoryCache.class),
                    Mockito.mock(OwnableService.class),
                    Mockito.mock(ApplicationEventPublisher.class)));
        }
    }

    @Test
    void setPermissions_timedPermissionOnlyInTable_setsPermissionInAlfrescoAgain() throws Throwable {
        // Arrange
        ACE timedAce = createTimedAce();
        // the merged ACL contains the table row, alfresco itself has no permission set anymore
        Mockito.doReturn(createAcl(timedAce)).when(permissionService).getPermissions(NODE_ID);
        Mockito.when(alfrescoPermissionService.getAllSetPermissions(any(org.alfresco.service.cmr.repository.NodeRef.class)))
                .thenReturn(Set.of());
        Mockito.doNothing().when(permissionService).setPermissions(anyString(), anyString(), any(String[].class), any());
        Mockito.doNothing().when(permissionService).createNotifyObject(anyString(), any(), anyString());

        // Act
        permissionService.setPermissions(NODE_ID, List.of(timedAce), null, null, false, false);

        // Assert
        Mockito.verify(permissionService).setPermissions(eq(NODE_ID), eq(AUTHORITY), eq(new String[]{PERMISSION}), isNull());
    }

    @Test
    void setPermissions_timedPermissionAlsoInAlfresco_doesNotChangeAlfresco() throws Throwable {
        // Arrange
        ACE timedAce = createTimedAce();
        AccessPermission alfrescoPermission = Mockito.mock(AccessPermission.class);
        Mockito.when(alfrescoPermission.isInherited()).thenReturn(false);
        Mockito.when(alfrescoPermission.getAuthority()).thenReturn(AUTHORITY);
        Mockito.when(alfrescoPermission.getPermission()).thenReturn(PERMISSION);
        Mockito.doReturn(createAcl(timedAce)).when(permissionService).getPermissions(NODE_ID);
        Mockito.when(alfrescoPermissionService.getAllSetPermissions(any(org.alfresco.service.cmr.repository.NodeRef.class)))
                .thenReturn(Set.of(alfrescoPermission));

        // Act
        permissionService.setPermissions(NODE_ID, List.of(timedAce), null, null, false, false);

        // Assert
        Mockito.verify(permissionService, Mockito.never()).setPermissions(anyString(), anyString(), any(String[].class), any());
        Mockito.verify(permissionService, Mockito.never()).createNotifyObject(anyString(), any(), anyString());
        Mockito.verify(timedPermissionMapper, Mockito.atLeastOnce()).save(any(TimedPermission.class));
    }

    private static ACE createTimedAce() {
        ACE ace = new ACE(PERMISSION, AUTHORITY);
        ace.setTo(new Date().getTime() + 24L * 60 * 60 * 1000);
        return ace;
    }

    private static ACL createAcl(ACE... aces) {
        ACL acl = new ACL();
        acl.setAces(aces);
        return acl;
    }
}
