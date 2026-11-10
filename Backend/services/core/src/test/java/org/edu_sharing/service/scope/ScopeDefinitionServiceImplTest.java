package org.edu_sharing.service.scope;

import java.lang.reflect.Method;
import org.edu_sharing.service.permission.annotation.Permission;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
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
    private ScopeService scopeService;
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

        scopeService = mock(ScopeService.class);
        ScopeServiceFactory factory = mock(ScopeServiceFactory.class);
        when(factory.getLocalService()).thenReturn(scopeService);
        ObjectProvider<ScopeServiceFactory> scopeServiceFactory = mock(ObjectProvider.class);
        when(scopeServiceFactory.getObject()).thenReturn(factory);

        underTest = new ScopeDefinitionServiceImpl(alfAuthorityService, nodeService, authorityService, transactionHelper,
                mock(ObjectProvider.class), organisationService, dictionaryService, permissionService, new ScopeProperties(dictionaryService),
                scopeServiceFactory);
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
        List<ScopeField> fields = List.of(ScopeField.builder().id("ccm:editorial_release").visibility(ScopeFieldVisibility.INTERNAL).publishedValues(List.of("released")).build());

        ScopeDefinition definition = underTest.createScope("wlo", "WLO", "mds_wlo", fields, null);

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
    void convertOrganization() throws Exception {
        groupProperties.put(OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR, FOLDER_REF);
        when(nodeService.exists(FOLDER_REF)).thenReturn(true);
        when(nodeService.getProperty(FOLDER_REF, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET))).thenReturn("mds_org");
        List<ScopeField> fields = List.of(ScopeField.builder().id("ccm:editorial_inventory").uploadValues(List.of("accepted")).build());

        ScopeDefinition definition = underTest.convertToScope(SCOPE, null, fields, null);

        assertEquals(SCOPE, definition.getScopeId());
        // no mds given: the one of the shared folder
        assertEquals("mds_org", definition.getMds());
        assertEquals("folder", definition.getFolderId());
        assertEquals(CCConstants.GROUP_TYPE_SCOPE, groupProperties.get(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE)));
        verify(organisationService, never()).createOrganization(any(), any(), any(), any());
        verify(permissionService).deletePermission(FOLDER_REF, SCOPE, PermissionService.CONSUMER);
        verify(permissionService).setPermission(FOLDER_REF, SCOPE, CCConstants.PERMISSION_COLLABORATOR, true);
        verify(scopeService).startFolderImport(SCOPE);
    }

    @Test
    void convertRejectsNoOrganizationsAndScopes() {
        // no shared folder
        assertThrows(IllegalArgumentException.class, () -> underTest.convertToScope(SCOPE, null, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> underTest.convertToScope("someone", null, List.of(), null));

        groupProperties.put(OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR, FOLDER_REF);
        when(nodeService.exists(FOLDER_REF)).thenReturn(true);
        for (String type : List.of(CCConstants.GROUP_TYPE_SCOPE, org.edu_sharing.alfresco.service.AuthorityService.MEDIA_CENTER_GROUP_TYPE)) {
            groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), type);
            assertThrows(IllegalArgumentException.class, () -> underTest.convertToScope(SCOPE, null, List.of(), null), type);
        }
        groupProperties.remove(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), CCConstants.CCM_VALUE_SCOPE_SAFE);
        assertThrows(IllegalArgumentException.class, () -> underTest.convertToScope(SCOPE, null, List.of(), null));

        verifyNoInteractions(permissionService, scopeService);
    }

    /**
     * enforced by PermissionChecking (proxy), not inside the methods
     */
    @Test
    void onlyAdminsCreateAndChangeScopes() throws NoSuchMethodException {
        for (Method method : List.of(
                ScopeDefinitionServiceImpl.class.getMethod("createScope", String.class, String.class, String.class, List.class, ScopePresentation.class),
                ScopeDefinitionServiceImpl.class.getMethod("updateScope", String.class, String.class, List.class, ScopePresentation.class),
                ScopeDefinitionServiceImpl.class.getMethod("convertToScope", String.class, String.class, List.class, ScopePresentation.class),
                ScopeDefinitionServiceImpl.class.getMethod("getAllScopes"),
                ScopeDefinitionServiceImpl.class.getMethod("getScopeOnlyProperties"))) {
            Permission permission = method.getAnnotation(Permission.class);
            assertNotNull(permission, method.getName());
            assertTrue(permission.requiresGlobalAdmin(), method.getName());
        }
    }

    @Test
    void updateRecomputesTheEntries() {
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);
        List<ScopeField> fields = List.of(ScopeField.builder().id("ccm:editorial_inventory").publishedValues(List.of("accepted")).build());

        underTest.updateScope(SCOPE, "mds", fields, null);

        verify(scopeService).onDefinitionChanged(SCOPE);
    }

    @Test
    void presentationIsStoredAndValidated() {
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);
        ScopePresentation presentation = ScopePresentation.builder().cardColumns(List.of("cclom:title", "virtual:licenseicon")).build();

        ScopeDefinition definition = underTest.updateScope(SCOPE, "mds", List.of(), presentation);
        assertEquals(List.of("cclom:title", "virtual:licenseicon"), definition.getPresentation().getCardColumns());

        // empty settings remove the property, the mds default applies again
        assertNull(underTest.updateScope(SCOPE, "mds", List.of(), ScopePresentation.builder().cardColumns(List.of()).build()).getPresentation());
        assertNull(underTest.updateScope(SCOPE, "mds", List.of(), null).getPresentation());

        assertThrows(IllegalArgumentException.class, () -> underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().cardColumns(List.of("ccm:unknown")).build()));
        assertThrows(IllegalArgumentException.class, () -> underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().cardColumns(List.of("cclom:title", "cclom:title")).build()));
    }

    @Test
    void sortColumnsAreStoredAndValidated() {
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);
        ScopePresentation presentation = ScopePresentation.builder().sortColumns(List.of("score", "cclom:title")).build();

        ScopePresentation stored = underTest.updateScope(SCOPE, "mds", List.of(), presentation).getPresentation();
        assertEquals(List.of("score", "cclom:title"), stored.getSortColumns());
        // the card columns stay the mds default
        assertNull(stored.getCardColumns());

        assertThrows(IllegalArgumentException.class, () -> underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().sortColumns(List.of("ccm:unknown")).build()));
        assertThrows(IllegalArgumentException.class, () -> underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().sortColumns(List.of("score", "score")).build()));
    }

    @Test
    void scopeOnlyPropertiesAreNotSortable() {
        groupProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);
        AspectDefinition scopeOnly = mock(AspectDefinition.class);
        when(scopeOnly.getProperties()).thenReturn(Map.of(
                QName.createQName(CCConstants.CCM_PROP_EDITORIAL_INVENTORY), mock(PropertyDefinition.class)));
        when(dictionaryService.getAspect(QName.createQName(CCConstants.CCM_ASPECT_EDITORIAL_SCOPE_PROPERTIES))).thenReturn(scopeOnly);
        String inventory = CCConstants.getValidLocalName(CCConstants.CCM_PROP_EDITORIAL_INVENTORY);

        assertThrows(IllegalArgumentException.class, () -> underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().sortColumns(List.of("cm:modified", inventory)).build()));
        // card columns may show them
        assertEquals(List.of(inventory), underTest.updateScope(SCOPE, "mds", List.of(),
                ScopePresentation.builder().cardColumns(List.of(inventory)).build()).getPresentation().getCardColumns());
    }

    @Test
    void fieldsMustBeKnownByTheDictionary() {
        List<ScopeField> unknown = List.of(ScopeField.builder().id("ccm:unknown").build());
        List<ScopeField> twice = List.of(ScopeField.builder().id("ccm:a").build(), ScopeField.builder().id("ccm:a").build());

        assertThrows(IllegalArgumentException.class, () -> underTest.createScope("wlo", "WLO", "mds_wlo", unknown, null));
        assertThrows(IllegalArgumentException.class, () -> underTest.createScope("wlo", "WLO", "mds_wlo", twice, null));
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
