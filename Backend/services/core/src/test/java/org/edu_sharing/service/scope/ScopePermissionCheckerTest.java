package org.edu_sharing.service.scope;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScopePermissionCheckerTest {

    static final String SCOPE = "GROUP_ORG_kunde_a";
    static final List<String> ALL = List.of(
            CCConstants.PERMISSION_READ, CCConstants.PERMISSION_READ_PREVIEW, CCConstants.PERMISSION_READ_ALL,
            CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_DOWNLOAD_CONTENT,
            CCConstants.PERMISSION_WRITE, CCConstants.PERMISSION_CC_PUBLISH, CCConstants.PERMISSION_COMMENT,
            CCConstants.PERMISSION_RATE, CCConstants.PERMISSION_EMBED, CCConstants.PERMISSION_DELETE);

    private ScopesSettings settings;
    private ScopeContextResolver resolver;
    private ScopeService scopeService;
    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private MockedStatic<NodeServiceFactory> nodeServiceFactory;
    private ScopePermissionChecker underTest;

    @BeforeEach
    void setUp() {
        settings = new ScopesSettings();
        settings.setEnabled(true);
        resolver = mock(ScopeContextResolver.class);
        scopeService = mock(ScopeService.class);
        when(scopeService.isPublishedInScope(SCOPE, "published")).thenReturn(true);
        ScopeServiceFactory scopeServiceFactory = mock(ScopeServiceFactory.class);
        when(scopeServiceFactory.getLocalService()).thenReturn(scopeService);

        NodeService nodeService = mock(NodeService.class);
        when(nodeService.getOriginalNode(anyString())).thenAnswer(i -> new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, i.getArgument(0)));
        // collection reference / published copy of the published original
        when(nodeService.getOriginalNode("reference")).thenReturn(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "published"));
        NodeServiceFactory factory = mock(NodeServiceFactory.class);
        when(factory.getLocalService()).thenReturn(nodeService);
        nodeServiceFactory = Mockito.mockStatic(NodeServiceFactory.class);
        nodeServiceFactory.when(NodeServiceFactory::getInstance).thenReturn(factory);
        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtil.when(() -> AuthenticationUtil.runAsSystem(any()))
                .thenAnswer(i -> ((AuthenticationUtil.RunAsWork<?>) i.getArgument(0)).doWork());

        underTest = new ScopePermissionChecker(settings, resolver, scopeServiceFactory);
    }

    @AfterEach
    void tearDown() {
        authenticationUtil.close();
        nodeServiceFactory.close();
    }

    private void activeScope(ScopeRole role) {
        when(resolver.resolve()).thenReturn(Optional.of(ActiveScope.builder()
                .definition(ScopeDefinition.builder().scopeId(SCOPE).build())
                .role(role)
                .build()));
    }

    @ParameterizedTest
    @EnumSource(ScopeRole.class)
    void publishedEntryGrantsConsumerPermissionsForEveryRole(ScopeRole role) {
        activeScope(role);

        assertEquals(ALL.stream().filter(CCConstants.getUsagePermissions()::contains).toList(),
                underTest.getGrantedPermissions("published", ALL));
    }

    @Test
    void neverWriteDeleteOrCcPublish() {
        activeScope(ScopeRole.EDITOR);

        assertEquals(List.of(), underTest.getGrantedPermissions("published", List.of(
                CCConstants.PERMISSION_WRITE, CCConstants.PERMISSION_CC_PUBLISH, CCConstants.PERMISSION_DELETE)));
    }

    @Test
    void unpublishedEntriesGrantNothingAlsoForEditors() {
        activeScope(ScopeRole.EDITOR);
        assertEquals(List.of(), underTest.getGrantedPermissions("unpublished", ALL));
    }

    @Test
    void referencesUseTheOriginal() {
        activeScope(ScopeRole.GUEST);
        assertEquals(List.of(CCConstants.PERMISSION_READ), underTest.getGrantedPermissions("reference", List.of(CCConstants.PERMISSION_READ)));
    }

    @Test
    void noEffectWithoutActiveScopeOrWhileDisabled() {
        when(resolver.resolve()).thenReturn(Optional.empty());
        assertEquals(List.of(), underTest.getGrantedPermissions("published", ALL));

        activeScope(ScopeRole.GUEST);
        settings.setEnabled(false);
        assertEquals(List.of(), underTest.getGrantedPermissions("published", ALL));
        verifyNoInteractions(scopeService);
    }
}
