package org.edu_sharing.service.authentication;

import com.typesafe.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authentication.sso.config.MappingGroup;
import org.edu_sharing.service.authentication.sso.config.MappingRoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ESD-678: an authentication by a rendering service carries no SSO attributes, so every group condition
 * evaluates to false. It must therefore never change users or group memberships (RS1 and RS2).
 */
class SSOAuthorityMapperRenderServiceTest {

    private static final String APP_ID = "render-app";
    private static final String USER = "sso-user";
    private static final String GROUP = "mapped-group";
    private static final String GROUP_AUTHORITY = "GROUP_" + GROUP;

    private MockedStatic<AlfAppContextGate> alfAppContextGate;
    private MockedStatic<ApplicationInfoList> applicationInfoList;
    private MockedStatic<LightbendConfigLoader> lightbend;

    private AuthorityService authorityService;
    private SSOAuthorityMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        authorityService = mock(AuthorityService.class);
        PersonService personService = mock(PersonService.class);
        MutableAuthenticationService authenticationService = mock(MutableAuthenticationService.class);
        NodeService nodeService = mock(NodeService.class);
        GuestService guestService = mock(GuestService.class);

        NodeRef personRef = new NodeRef("workspace://SpacesStore/person");
        when(personService.personExists(USER)).thenReturn(true);
        when(personService.getPersonOrNull(USER)).thenReturn(personRef);
        when(nodeService.getProperty(personRef, ContentModel.PROP_USERNAME)).thenReturn(USER);
        when(authenticationService.authenticationExists(USER)).thenReturn(true);
        // the user is currently a member of the mapped group
        when(authorityService.authorityExists(GROUP_AUTHORITY)).thenReturn(true);
        when(authorityService.getAuthoritiesForUser(USER)).thenReturn(Set.of(GROUP_AUTHORITY));

        // the user name is the only attribute a rendering service delivers, the group condition can not match
        MappingGroup mappingGroup = new MappingGroup();
        mappingGroup.setMapTo(GROUP);
        mappingGroup.setCondition(attributes -> false);
        MappingRoot mappingRoot = new MappingRoot();
        mappingRoot.setPersonMapping(Map.of("uid", CCConstants.CM_PROP_PERSON_USERNAME));
        mappingRoot.setGroupMapping(List.of(mappingGroup));

        mapper = new SSOAuthorityMapper();
        mapper.setMappingConfig(mappingRoot);
        mapper.setNodeService(nodeService);
        mapper.setUpdateMemberships(true);
        mapper.setCreateGroups(true);
        mapper.setUpdateUser(true);
        inject("authorityService", authorityService);
        inject("personService", personService);
        inject("authenticationService", authenticationService);
        inject("guestService", guestService);
        inject("transactionService", mock(TransactionService.class, Mockito.RETURNS_DEEP_STUBS));

        ApplicationInfo home = mock(ApplicationInfo.class);
        when(home.getAppId()).thenReturn("home");
        // ApplicationInfoList reads a cache bean from the Alfresco context in its static initializer
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mock(SimpleCache.class));
        alfAppContextGate = Mockito.mockStatic(AlfAppContextGate.class);
        alfAppContextGate.when(AlfAppContextGate::getApplicationContext).thenReturn(applicationContext);
        applicationInfoList = Mockito.mockStatic(ApplicationInfoList.class);
        applicationInfoList.when(ApplicationInfoList::getHomeRepository).thenReturn(home);

        Config config = mock(Config.class);
        when(config.getIsNull(anyString())).thenReturn(true);
        lightbend = Mockito.mockStatic(LightbendConfigLoader.class);
        lightbend.when(LightbendConfigLoader::get).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        applicationInfoList.close();
        alfAppContextGate.close();
        lightbend.close();
    }

    private void inject(String field, Object value) throws Exception {
        var f = SSOAuthorityMapper.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mapper, value);
    }

    private void registerApp(String type) {
        ApplicationInfo app = mock(ApplicationInfo.class);
        when(app.getAppId()).thenReturn(APP_ID);
        when(app.getType()).thenReturn(type);
        when(app.isAllowAdminLogin()).thenReturn(true);
        applicationInfoList.when(() -> ApplicationInfoList.getRepositoryInfoById(APP_ID)).thenReturn(app);
    }

    private String authenticateByApp() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("uid", USER);
        attributes.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
        attributes.put(SSOAuthorityMapper.PARAM_APP_ID, APP_ID);
        Method internal = SSOAuthorityMapper.class.getDeclaredMethod("mapAuthorityInternal", Map.class);
        internal.setAccessible(true);
        try {
            return (String) internal.invoke(mapper, attributes);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {ApplicationInfo.TYPE_RENDERSERVICE, ApplicationInfo.TYPE_RENDERSERVICE_2})
    void renderServiceDoesNotChangeUsersOrMemberships(String renderServiceType) throws Exception {
        registerApp(renderServiceType);

        assertEquals(USER, authenticateByApp());

        verify(authorityService, never()).removeAuthority(anyString(), anyString());
        verify(authorityService, never()).addAuthority(anyString(), anyString());
        verify(authorityService, never()).createAuthority(any(), anyString(), anyString(), any());
    }

    /** Guards the test itself: for any other application the very same input does remove the membership. */
    @ParameterizedTest
    @ValueSource(strings = {ApplicationInfo.TYPE_LMS})
    void otherApplicationsStillUpdateMemberships(String type) throws Exception {
        registerApp(type);

        assertEquals(USER, authenticateByApp());

        verify(authorityService).removeAuthority(GROUP_AUTHORITY, USER);
    }
}
