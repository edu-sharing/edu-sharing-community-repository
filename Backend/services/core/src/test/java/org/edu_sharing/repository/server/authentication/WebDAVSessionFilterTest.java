package org.edu_sharing.repository.server.authentication;

import com.typesafe.config.Config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Test class for WebDAVSessionFilter.
 * This class tests the behavior of the doFilter method to ensure it handles
 * session management according to the configuration.
 */
public class WebDAVSessionFilterTest {

    @Test
    public void testDoFilter_SessionDisabled_RequestAndResponseWrapped() throws IOException, jakarta.servlet.ServletException {
        // Arrange
        WebDAVSessionFilter filter = new WebDAVSessionFilter();
        LightbendConfigLoader configLoader = mock(LightbendConfigLoader.class);
        Config config = mock(Config.class);

        when(configLoader.getConfig()).thenReturn(config);
        when(config.hasPath("repository.webdav.session.enabled")).thenReturn(true);
        when(config.getBoolean("repository.webdav.session.enabled")).thenReturn(false);

        filter.setConfigLoader(configLoader);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockChain);

        // Assert
        verify(mockChain).doFilter(any(WebDAVSessionFilter.WebDAVSessionRequestWrapper.class), any(WebDAVSessionFilter.WebDAVSessionResponseWrapper.class));
        verifyNoMoreInteractions(mockChain);
    }

    @Test
    public void testDoFilter_SessionEnabled_RequestAndResponseNotWrapped() throws IOException, jakarta.servlet.ServletException {
        // Arrange
        WebDAVSessionFilter filter = new WebDAVSessionFilter();
        LightbendConfigLoader configLoader = mock(LightbendConfigLoader.class);
        Config config = mock(Config.class);

        when(configLoader.getConfig()).thenReturn(config);
        when(config.hasPath("repository.webdav.session.enabled")).thenReturn(true);
        when(config.getBoolean("repository.webdav.session.enabled")).thenReturn(true);

        filter.setConfigLoader(configLoader);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockChain);

        // Assert
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verifyNoMoreInteractions(mockChain);
    }

    @Test
    public void testDoFilter_NoConfigSessionEnabledHeader_RequestAndResponseNotWrapped() throws IOException, jakarta.servlet.ServletException {
        // Arrange
        WebDAVSessionFilter filter = new WebDAVSessionFilter();
        LightbendConfigLoader configLoader = mock(LightbendConfigLoader.class);

        when(configLoader.getConfig()).thenReturn(null);

        filter.setConfigLoader(configLoader);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockChain);

        // Assert
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verifyNoMoreInteractions(mockChain);
    }

    @Test
    public void testWebDAVSessionRequestWrapper_RemovesJSessionIDCookie() {
        // Arrange
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Cookie cookie1 = new Cookie("Test-Cookie", "Value1");
        Cookie cookie2 = new Cookie("JSESSIONID", "SessionValue");
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{cookie1, cookie2});

        WebDAVSessionFilter.WebDAVSessionRequestWrapper requestWrapper = new WebDAVSessionFilter.WebDAVSessionRequestWrapper(mockRequest);

        // Act
        Cookie[] filteredCookies = requestWrapper.getCookies();

        // Assert
        assert filteredCookies != null;
        assert filteredCookies.length == 1;
        assert filteredCookies[0].getName().equals("Test-Cookie");
    }

    @Test
    public void testWebDAVSessionRequestWrapper_RemovesJSessionIDHeader() {
        // Arrange
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("Cookie")).thenReturn("Test-Cookie=Value1; JSESSIONID=SessionValue");

        WebDAVSessionFilter.WebDAVSessionRequestWrapper requestWrapper = new WebDAVSessionFilter.WebDAVSessionRequestWrapper(mockRequest);

        // Act
        String filteredHeader = requestWrapper.getHeader("Cookie");

        // Assert
        assert filteredHeader != null;
        assert !filteredHeader.contains("JSESSIONID");
        assert filteredHeader.contains("Test-Cookie=Value1");
    }

    @Test
    public void testWebDAVSessionResponseWrapper_DoesNotAddJSessionIDHeader() {
        // Arrange
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        WebDAVSessionFilter.WebDAVSessionResponseWrapper responseWrapper = new WebDAVSessionFilter.WebDAVSessionResponseWrapper(mockResponse);

        // Act
        responseWrapper.addHeader("Set-Cookie", "JSESSIONID=SessionValue");

        // Assert
        verify(mockResponse, never()).addHeader("Set-Cookie", "JSESSIONID=SessionValue");
    }

    @Test
    public void testWebDAVSessionResponseWrapper_AddsNonSessionHeaders() {
        // Arrange
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        WebDAVSessionFilter.WebDAVSessionResponseWrapper responseWrapper = new WebDAVSessionFilter.WebDAVSessionResponseWrapper(mockResponse);

        // Act
        responseWrapper.addHeader("Set-Cookie", "OtherCookie=Value");

        // Assert
        verify(mockResponse).addHeader("Set-Cookie", "OtherCookie=Value");
    }
}