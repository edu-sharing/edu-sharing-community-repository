package org.edu_sharing.service.authentication;

import com.typesafe.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ESD-678: an authentication by a rendering service carries no SSO attributes, so no group mapping can match.
 * It must therefore never change users or group memberships (RS1 and RS2).
 */
class SSOAuthorityMapperRenderServiceTest {

    private static final String APP_ID = "render-app";
    private static final String USER = "sso-user";
    private static final String GROUP = "mapped-group";
    private static final String GROUP_AUTHORITY = "GROUP_" + GROUP;

    private MockedStatic<AlfAppContextGate> alfAppContextGate;
    private MockedStatic<ApplicationInfoList> applicationInfoList;

    private AuthorityService authorityService;
    private SSOAuthorityMapper mapper;

    @BeforeEach
    void setUp() {
        authorityService = mock(AuthorityService.class);
        PersonService personService = mock(PersonService.class);
        MutableAuthenticationService authenticationService = mock(MutableAuthenticationService.class);
        NodeService nodeService = mock(NodeService.class);

        NodeRef personRef = new NodeRef("workspace://SpacesStore/person");
        when(personService.personExists(USER)).thenReturn(true);
        when(personService.getPersonOrNull(USER)).thenReturn(personRef);
        when(nodeService.getProperty(personRef, ContentModel.PROP_USERNAME)).thenReturn(USER);
        when(authenticationService.authenticationExists(USER)).thenReturn(true);
        // the user is currently a member of the mapped group
        when(authorityService.authorityExists(GROUP_AUTHORITY)).thenReturn(true);
        when(authorityService.getAuthoritiesForUser(USER)).thenReturn(Set.of(GROUP_AUTHORITY));

        // the user name is the only attribute a rendering service delivers, the group attribute is missing
        Mapping.Group mappingGroup = new Mapping.Group();
        mappingGroup.setGroup(GROUP);
        mappingGroup.setAttribute("affiliation");
        mappingGroup.setMatcher("teacher");
        Mapping mapping = new Mapping();
        mapping.setPreferRemoteUser(false);
        mapping.setSettings(new SSOAuthorityMapper.Configuration());
        mapping.getPerson().put("uid", CCConstants.CM_PROP_PERSON_USERNAME);
        mapping.getGroup().put(GROUP, mappingGroup);
        SSOMappingProvider ssoMappingProvider = mock(SSOMappingProvider.class);
        when(ssoMappingProvider.getMapping(any())).thenReturn(mapping);

        Config config = mock(Config.class);
        when(config.getIsNull(anyString())).thenReturn(true);
        LightbendConfigLoader lightbendConfigLoader = mock(LightbendConfigLoader.class);
        when(lightbendConfigLoader.getConfig()).thenReturn(config);

        mapper = new SSOAuthorityMapper(
                authorityService,
                personService,
                authenticationService,
                mock(TransactionService.class, Mockito.RETURNS_DEEP_STUBS),
                mock(OrganisationService.class),
                nodeService,
                mock(GuestService.class),
                lightbendConfigLoader,
                ssoMappingProvider,
                Optional.empty(),
                Optional.empty());

        ApplicationInfo home = mock(ApplicationInfo.class);
        when(home.getAppId()).thenReturn("home");
        // ApplicationInfoList reads a cache bean from the Alfresco context in its static initializer
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mock(SimpleCache.class));
        alfAppContextGate = Mockito.mockStatic(AlfAppContextGate.class);
        alfAppContextGate.when(AlfAppContextGate::getApplicationContext).thenReturn(applicationContext);
        applicationInfoList = Mockito.mockStatic(ApplicationInfoList.class);
        applicationInfoList.when(ApplicationInfoList::getHomeRepository).thenReturn(home);
    }

    @AfterEach
    void tearDown() {
        // close whatever setUp managed to open: a static mock left registered breaks every later test of the thread
        for (MockedStatic<?> mock : new MockedStatic<?>[]{applicationInfoList, alfAppContextGate}) {
            if (mock != null) {
                mock.close();
            }
        }
    }

    private void registerApp(String type) {
        ApplicationInfo app = mock(ApplicationInfo.class);
        when(app.getAppId()).thenReturn(APP_ID);
        when(app.getType()).thenReturn(type);
        when(app.isAllowAdminLogin()).thenReturn(true);
        applicationInfoList.when(() -> ApplicationInfoList.getRepositoryInfoById(APP_ID)).thenReturn(app);
    }

    private String authenticateByApp() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("uid", USER);
        attributes.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
        attributes.put(SSOAuthorityMapper.PARAM_AUTHBYAPP_APP_ID, APP_ID);
        return mapper.mapAuthority(attributes);
    }

    @ParameterizedTest
    @ValueSource(strings = {ApplicationInfo.TYPE_RENDERSERVICE, ApplicationInfo.TYPE_RENDERSERVICE_2})
    void renderServiceDoesNotChangeUsersOrMemberships(String renderServiceType) {
        // Arrange
        registerApp(renderServiceType);

        // Act
        String result = authenticateByApp();

        // Assert
        assertEquals(USER, result);
        verify(authorityService, never()).removeAuthority(anyString(), anyString());
        verify(authorityService, never()).addAuthority(anyString(), anyString());
        verify(authorityService, never()).createAuthority(any(), anyString(), anyString(), any());
    }

    /** Guards the test itself: for any other application the very same input does remove the membership. */
    @ParameterizedTest
    @ValueSource(strings = {ApplicationInfo.TYPE_LMS})
    void otherApplicationsStillUpdateMemberships(String type) {
        // Arrange
        registerApp(type);

        // Act
        String result = authenticateByApp();

        // Assert
        assertEquals(USER, result);
        verify(authorityService).removeAuthority(GROUP_AUTHORITY, USER);
    }
}
