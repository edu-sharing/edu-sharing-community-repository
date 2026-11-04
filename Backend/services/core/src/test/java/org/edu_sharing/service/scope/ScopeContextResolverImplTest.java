package org.edu_sharing.service.scope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScopeContextResolverImplTest {

    static final String DOMAIN_SCOPE = "GROUP_ORG_kunde_a";
    static final String OWN_SCOPE = "GROUP_ORG_wlo";

    private final Map<String, Object> requestAttributes = new HashMap<>();
    private final Map<String, Object> sessionAttributes = new HashMap<>();
    private ScopesSettings settings;
    private ScopeDefinitionService definitionService;
    private GuestService guestService;
    private org.edu_sharing.service.authority.AuthorityService authorityService;
    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private String contextId;
    private ScopeContextResolverImpl underTest;

    private static Object attributes(Map<String, Object> map, org.mockito.invocation.InvocationOnMock i) {
        switch (i.getMethod().getName()) {
            case "getAttribute" -> {
                return map.get((String) i.getArgument(0));
            }
            case "setAttribute" -> map.put(i.getArgument(0), i.getArgument(1));
            case "removeAttribute" -> map.remove((String) i.getArgument(0));
            default -> {
            }
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        settings = new ScopesSettings();
        settings.setEnabled(true);
        definitionService = mock(ScopeDefinitionService.class);
        when(definitionService.getDefinition(anyString())).thenReturn(Optional.empty());
        for (String scope : List.of(DOMAIN_SCOPE, OWN_SCOPE)) {
            when(definitionService.getDefinition(scope)).thenReturn(Optional.of(ScopeDefinition.builder().scopeId(scope).displayName(scope).build()));
        }
        when(definitionService.getScopesOfCurrentUser()).thenReturn(List.of(ScopeDefinition.builder().scopeId(OWN_SCOPE).displayName(OWN_SCOPE).build()));
        when(definitionService.isMember(OWN_SCOPE)).thenReturn(true);

        guestService = mock(GuestService.class);
        GuestConfig guest = new GuestConfig();
        guest.setEnabled(true);
        guest.setUsername("guest_kunde_a");
        when(guestService.getConfig("kunde_a")).thenReturn(guest);

        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("editor");

        HttpServletRequest request = mock(HttpServletRequest.class, i -> attributes(requestAttributes, i));
        HttpSession session = mock(HttpSession.class, i -> attributes(sessionAttributes, i));
        authorityService = mock(org.edu_sharing.service.authority.AuthorityService.class);
        underTest = new ScopeContextResolverImpl(settings, definitionService, guestService, authorityService) {
            @Override
            protected HttpServletRequest getRequest() {
                return request;
            }

            @Override
            protected HttpSession getSession(boolean create) {
                return session;
            }

            @Override
            protected String getCurrentContextId() {
                return contextId;
            }

            @Override
            protected String getContextScopeGroup(String contextId) {
                return "kunde_a".equals(contextId) ? DOMAIN_SCOPE : null;
            }
        };
    }

    @AfterEach
    void tearDown() {
        authenticationUtil.close();
    }

    private Optional<ActiveScope> resolveFresh() {
        requestAttributes.clear();
        return underTest.resolve();
    }

    @Test
    void runWithScopeSetsTheActiveScopeOfTheThread() throws Exception {
        // a job thread: no domain, no selection
        assertTrue(resolveFresh().isEmpty());

        ActiveScope inJob = underTest.runWithScope(OWN_SCOPE, () -> resolveFresh().orElseThrow());
        assertEquals(OWN_SCOPE, inJob.getScopeId());
        assertEquals(ScopeRole.EDITOR, inJob.getRole());
        // not a member: read only
        assertEquals(ScopeRole.TEMPORARY_READER, underTest.runWithScope(DOMAIN_SCOPE, () -> resolveFresh().orElseThrow()).getRole());

        assertTrue(resolveFresh().isEmpty());
        assertThrows(ScopeNotFoundException.class, () -> underTest.runWithScope("GROUP_gone", () -> null));
    }

    /**
     * domain x membership x selection of the switcher (concept §2.1)
     */
    @ParameterizedTest(name = "context={0} member={1} user={2} selected={3} -> {4} {5}")
    @CsvSource(nullValues = "-", value = {
            // no domain scope: the selection of the switcher decides
            "-,       false, editor,        -,                -,                 -",
            "-,       false, editor,        GROUP_ORG_wlo,    GROUP_ORG_wlo,     EDITOR",
            // domain scope wins over the selection
            "kunde_a, true,  editor,        GROUP_ORG_wlo,    GROUP_ORG_kunde_a, EDITOR",
            "kunde_a, false, editor,        GROUP_ORG_wlo,    GROUP_ORG_kunde_a, TEMPORARY_READER",
            "kunde_a, false, editor,        -,                GROUP_ORG_kunde_a, TEMPORARY_READER",
            "kunde_a, false, guest_kunde_a, -,                GROUP_ORG_kunde_a, GUEST",
            // a context without scope behaves like no context
            "other,   false, editor,        GROUP_ORG_wlo,    GROUP_ORG_wlo,     EDITOR",
    })
    void resolution(String context, boolean member, String user, String selected, String expectedScope, ScopeRole expectedRole) {
        contextId = context;
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(user);
        when(definitionService.isMember(DOMAIN_SCOPE)).thenReturn(member);
        if (selected != null) {
            sessionAttributes.put(CCConstants.SESSION_SCOPE_ACTIVE, selected);
        }

        Optional<ActiveScope> scope = resolveFresh();

        assertEquals(expectedScope, scope.map(ActiveScope::getScopeId).orElse(null));
        assertEquals(expectedRole, scope.map(ActiveScope::getRole).orElse(null));
    }

    @Test
    void orgAdminsOfTheScopeGroup() {
        sessionAttributes.put(CCConstants.SESSION_SCOPE_ACTIVE, OWN_SCOPE);
        assertFalse(resolveFresh().orElseThrow().isOrgAdmin());

        when(authorityService.hasAdminAccessToGroup(OWN_SCOPE)).thenReturn(true);
        assertTrue(resolveFresh().orElseThrow().isOrgAdmin());

        // never for guests or temporary readers of a context
        contextId = "kunde_a";
        when(authorityService.hasAdminAccessToGroup(DOMAIN_SCOPE)).thenReturn(true);
        assertFalse(resolveFresh().orElseThrow().isOrgAdmin());
    }

    @Test
    void selectionOfAScopeThatIsNoLongerOwnIsDropped() {
        sessionAttributes.put(CCConstants.SESSION_SCOPE_ACTIVE, OWN_SCOPE);
        when(definitionService.isMember(OWN_SCOPE)).thenReturn(false);

        assertTrue(resolveFresh().isEmpty());
        assertNull(sessionAttributes.get(CCConstants.SESSION_SCOPE_ACTIVE));
    }

    @Test
    void disabledFeatureResolvesToGlobal() {
        settings.setEnabled(false);
        contextId = "kunde_a";
        sessionAttributes.put(CCConstants.SESSION_SCOPE_ACTIVE, OWN_SCOPE);

        assertTrue(resolveFresh().isEmpty());
    }

    @Test
    void resultIsCachedPerRequest() {
        sessionAttributes.put(CCConstants.SESSION_SCOPE_ACTIVE, OWN_SCOPE);
        underTest.resolve();
        underTest.resolve();

        verify(definitionService, times(1)).getDefinition(OWN_SCOPE);
    }

    @Test
    void switcher() {
        underTest.setSelectedScope(OWN_SCOPE);
        assertEquals(OWN_SCOPE, sessionAttributes.get(CCConstants.SESSION_SCOPE_ACTIVE));

        assertThrows(InsufficientPermissionException.class, () -> underTest.setSelectedScope(DOMAIN_SCOPE));
        assertThrows(ScopeNotFoundException.class, () -> underTest.setSelectedScope("GROUP_unknown"));

        underTest.setSelectedScope(null);
        assertNull(sessionAttributes.get(CCConstants.SESSION_SCOPE_ACTIVE));
    }

    @Test
    void availableScopesContainTheTemporaryDomainScope() {
        contextId = "kunde_a";

        List<ScopeInfo> scopes = underTest.getAvailableScopes();

        assertEquals(List.of(OWN_SCOPE, DOMAIN_SCOPE), scopes.stream().map(ScopeInfo::getScopeId).toList());
        assertFalse(scopes.get(0).isTemporary());
        assertTrue(scopes.get(1).isTemporary());
        assertEquals(ScopeRole.TEMPORARY_READER, scopes.get(1).getRole());
    }
}
