package org.alfresco.repo.webdav;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class Edu_SharingBeanProxyFilterTest {

    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private MockedStatic<ApplicationInfoList> applicationInfoList;
    private MockedStatic<AuthorityServiceFactory> authorityServiceFactory;
    private AuthorityService authorityService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private FilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authorityService = mock(AuthorityService.class);
        applicationInfoList = Mockito.mockStatic(ApplicationInfoList.class);
        authorityServiceFactory = Mockito.mockStatic(AuthorityServiceFactory.class);
        AuthorityServiceFactory factory = mock(AuthorityServiceFactory.class);
        authorityServiceFactory.when(AuthorityServiceFactory::getInstance).thenReturn(factory);
        when(factory.getLocalService()).thenReturn(authorityService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        chain = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(session);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @AfterEach
    void tearDown() {
        authenticationUtil.close();
        applicationInfoList.close();
        authorityServiceFactory.close();
    }

    private FilterChain guard(boolean internal) {
        applicationInfoList.when(() -> ApplicationInfoList.isInternalPortRequest(request)).thenReturn(internal);
        return Edu_SharingBeanProxyFilter.twoFactorGuard(chain);
    }

    @Test
    void externalRequestWith2FaIsBlocked() throws Exception {
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        when(authorityService.is2FaActive("user")).thenReturn(true);

        guard(false).doFilter(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(session).invalidate();
        authenticationUtil.verify(AuthenticationUtil::clearCurrentSecurityContext);
        verifyNoInteractions(chain);
    }

    @Test
    void externalRequestWithout2FaPasses() throws Exception {
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        when(authorityService.is2FaActive("user")).thenReturn(false);

        guard(false).doFilter(request, response);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void internalRequestWith2FaPasses() throws Exception {
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        when(authorityService.is2FaActive("user")).thenReturn(true);

        guard(true).doFilter(request, response);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(authorityService);
    }

    @Test
    void unauthenticatedRequestPasses() throws Exception {
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(null);

        guard(false).doFilter(request, response);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(authorityService);
    }

    @Test
    void failingStatusLookupIsBlocked() throws Exception {
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        when(authorityService.is2FaActive("user")).thenThrow(new RuntimeException("boom"));

        guard(false).doFilter(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoInteractions(chain);
    }
}
