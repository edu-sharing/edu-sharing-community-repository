package org.edu_sharing.repository.server.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class LoginRedirectTest {

    private HttpServletRequest request;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
        when(request.getContextPath()).thenReturn("/edu-sharing");
        when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/edu-sharing/components/render/abc",
            "/edu-sharing/components/collections?id=abc&scope=MY",
            "/edu-sharing//evil.example.org",
    })
    void acceptsPathsOfThisApp(String target) {
        assertEquals(target, LoginRedirect.validate(request, target));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example.org/edu-sharing/components/render/abc",
            "//evil.example.org/edu-sharing/",
            "/\\evil.example.org/edu-sharing/",
            "/edu-sharing\\..\\x",
            "/edu-sharing/../other-app/",
            "/other-app/components",
            "/edu-sharing",
            "javascript:alert(1)",
            "/edu-sharing/components\r\nSet-Cookie: x=1",
            " ",
    })
    void rejectsEverythingElse(String target) {
        assertNull(LoginRedirect.validate(request, target));
    }

    @Test
    void rejectsNullAndOverlongTargets() {
        assertNull(LoginRedirect.validate(request, null));
        assertNull(LoginRedirect.validate(request, "/edu-sharing/" + "x".repeat(5000)));
    }

    @Test
    void remembersOnlyValidTargets() {
        when(request.getParameter(LoginRedirect.PARAM)).thenReturn("https://evil.example.org/");
        LoginRedirect.remember(request);
        verify(session, never()).setAttribute(eq(LoginRedirect.SESSION_ATTRIBUTE), any());

        when(request.getParameter(LoginRedirect.PARAM)).thenReturn("/edu-sharing/components/render/abc");
        LoginRedirect.remember(request);
        verify(session).setAttribute(LoginRedirect.SESSION_ATTRIBUTE, "/edu-sharing/components/render/abc");
    }

    @Test
    void resolvesTheParameterFirst() {
        when(request.getParameter(LoginRedirect.PARAM)).thenReturn("/edu-sharing/components/render/param");
        when(session.getAttribute(LoginRedirect.SESSION_ATTRIBUTE)).thenReturn("/edu-sharing/components/render/remembered");
        assertEquals("/edu-sharing/components/render/param", LoginRedirect.resolve(request));
        // the remembered one is consumed either way
        verify(session).removeAttribute(LoginRedirect.SESSION_ATTRIBUTE);
    }

    @Test
    void thenTheRememberedTarget() {
        when(session.getAttribute(LoginRedirect.SESSION_ATTRIBUTE)).thenReturn("/edu-sharing/components/render/remembered");
        when(session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL)).thenReturn("/edu-sharing/components/silent");
        assertEquals("/edu-sharing/components/render/remembered", LoginRedirect.resolve(request));
    }

    @Test
    void thenTheSilentLoginTarget() {
        when(session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL)).thenReturn("/edu-sharing/components/silent");
        assertEquals("/edu-sharing/components/silent", LoginRedirect.resolve(request));
    }

    @Test
    void ignoresAnInvalidParameterAndFallsBack() {
        when(request.getParameter(LoginRedirect.PARAM)).thenReturn("//evil.example.org/");
        when(session.getAttribute(LoginRedirect.SESSION_ATTRIBUTE)).thenReturn("/edu-sharing/components/render/remembered");
        assertEquals("/edu-sharing/components/render/remembered", LoginRedirect.resolve(request));
    }

    @Test
    void nothingWithoutSessionAndParameter() {
        when(request.getSession(false)).thenReturn(null);
        assertNull(LoginRedirect.resolve(request));
    }
}
