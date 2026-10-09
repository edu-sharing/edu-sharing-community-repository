package org.edu_sharing.service.scope;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScopeDefinitionServiceImplTest {

    static final String SCOPE = "GROUP_ORG_wlo";
    static final NodeRef GROUP_REF = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "group");
    static final NodeRef FOLDER_REF = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "folder");

    private final Map<QName, Serializable> groupProperties = new HashMap<>();
    private org.alfresco.service.cmr.security.AuthorityService alfAuthorityService;
    private NodeService nodeService;
    private AuthorityService authorityService;
    private OrganisationService organisationService;
    private DictionaryService dictionaryService;
    private PermissionService permissionService;
    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private ScopeDefinitionServiceImpl underTest;

    @BeforeEach
    void setUp() throws Throwable {
        alfAuthorityService = mock(org.alfresco.service.cmr.security.AuthorityService.class);
        when(alfAuthorityService.getAuthorityNodeRef(SCOPE)).thenReturn(GROUP_REF);
        when(alfAuthorityService.getAuthorityDisplayName(SCOPE)).thenReturn("WLO");

        nodeService = mock(NodeService.class);
        when(nodeService.getProperties(GROUP_REF)).thenAnswer(i -> new HashMap<>(groupProperties));
        when(nodeService.getProperty(GROUP_REF, OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR)).thenReturn(FOLDER_REF);
        doAnswer(i -> groupProperties.put(i.getArgument(1), i.getArgument(2))).when(nodeService).setProperty(eq(GROUP_REF), any(), any());
        doAnswer(i -> {
            groupProperties.putAll(i.getArgument(2));
            return null;
        }).when(nodeService).addAspect(eq(GROUP_REF), any(), any());

        authorityService = mock(AuthorityService.class);
        organisationService = mock(OrganisationService.class);
        when(organisationService.createOrganization("wlo", "WLO", "mds_wlo", null)).thenReturn("ORG_wlo");
        dictionaryService = mock(DictionaryService.class);
        when(dictionaryService.getProperty(any(QName.class))).thenReturn(mock(PropertyDefinition.class));
        when(dictionaryService.getProperty(QName.createQName(CCConstants.getValidGlobalName("ccm:unknown")))).thenReturn(null);
        permissionService = mock(PermissionService.class);

        RetryingTransactionHelper transactionHelper = mock(RetryingTransactionHelper.class);
        when(transactionHelper.doInTransaction(any())).thenAnswer(i -> ((RetryingTransactionHelper.RetryingTransactionCallback<?>) i.getArgument(0)).execute());
        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtil.when(() -> AuthenticationUtil.runAsSystem(any()))
                .thenAnswer(i -> ((AuthenticationUtil.RunAsWork<?>) i.getArgument(0)).doWork());

        underTest = new ScopeDefinitionServiceImpl(alfAuthorityService, nodeService, authorityService, transactionHelper,
                mock(ObjectProvider.class), organisationService, dictionaryService, permissionService);
    }

    @AfterEach
    void tearDown() {
        authenticationUtil.close();
    }

    @Test
    void groupsWithoutScopeTypeAreNoScopes() {
        assertTrue(underTest.getDefinition(SCOPE).isEmpty());
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.EDITORIAL_GROUP_TYPE);
        assertTrue(underTest.getDefinition(SCOPE).isEmpty());
        assertTrue(underTest.getDefinition("someone").isEmpty());
    }

    @Test
    void createScope() {
        when(authorityService.isGlobalAdmin()).thenReturn(true);
        List<ScopeField> fields = List.of(ScopeField.builder().id("ccm:editorial_release").visibility(ScopeFieldVisibility.INTERNAL).publishedValues(List.of("released")).build());

        ScopeDefinition definition = underTest.createScope("wlo", "WLO", "mds_wlo", fields);

        assertEquals(SCOPE, definition.getScopeId());
        assertEquals("WLO", definition.getDisplayName());
        assertEquals("mds_wlo", definition.getMds());
        assertEquals(fields, definition.getFields());
        assertEquals("folder", definition.getFolderId());
        verify(nodeService).addAspect(eq(GROUP_REF), eq(QName.createQName(CCConstants.CCM_ASPECT_SCOPE_GROUP)), any());
        // shared folder: Collaborator instead of the organization default Consumer
        verify(permissionService).deletePermission(FOLDER_REF, SCOPE, PermissionService.CONSUMER);
        verify(permissionService).setPermission(FOLDER_REF, SCOPE, CCConstants.PERMISSION_COLLABORATOR, true);
    }

    @Test
    void onlyAdminsCreateAndChangeScopes() {
        assertThrows(InsufficientPermissionException.class, () -> underTest.createScope("wlo", "WLO", "mds_wlo", List.of()));
        assertThrows(InsufficientPermissionException.class, () -> underTest.updateScope(SCOPE, "mds", List.of()));
        assertThrows(InsufficientPermissionException.class, () -> underTest.getAllScopes());
        verifyNoInteractions(organisationService);
    }

    @Test
    void fieldsMustBeKnownByTheDictionary() {
        when(authorityService.isGlobalAdmin()).thenReturn(true);
        List<ScopeField> unknown = List.of(ScopeField.builder().id("ccm:unknown").build());
        List<ScopeField> twice = List.of(ScopeField.builder().id("ccm:a").build(), ScopeField.builder().id("ccm:a").build());

        assertThrows(IllegalArgumentException.class, () -> underTest.createScope("wlo", "WLO", "mds_wlo", unknown));
        assertThrows(IllegalArgumentException.class, () -> underTest.createScope("wlo", "WLO", "mds_wlo", twice));
        verifyNoInteractions(organisationService);
    }

    @Test
    void baseQueryOnlyForOrgAdminsAndVersioned() {
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);
        ScopeBaseQuery query = ScopeBaseQuery.builder().queries(List.of(ScopeBaseQuery.Query.builder().mds("mds").query("ngsearch").build())).build();

        assertThrows(InsufficientPermissionException.class, () -> underTest.setBaseQuery(SCOPE, query));

        when(authorityService.hasAdminAccessToGroup(SCOPE)).thenReturn(true);
        assertEquals(1, underTest.setBaseQuery(SCOPE, query).getVersion());
        assertEquals(2, underTest.setBaseQuery(SCOPE, query).getVersion());
        ScopeBaseQuery stored = underTest.getDefinition(SCOPE).orElseThrow().getBaseQuery();
        assertEquals(2, stored.getVersion());
        assertEquals(query.getQueries(), stored.getQueries());
        // one value per query in the multi value property
        assertEquals(1, ((List<?>) groupProperties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_BASE_QUERY))).size());
        assertThrows(ScopeNotFoundException.class, () -> underTest.setBaseQuery("GROUP_other", query));
    }
}
