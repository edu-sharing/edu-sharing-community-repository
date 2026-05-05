package org.edu_sharing.repository.server.authentication;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.web.SpringFilter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter to optionally disable sessions for WebDAV.
 * It removes JSESSIONID cookies from requests and Set-Cookie headers from responses.
 */
@Slf4j
public class WebDAVSessionFilter extends SpringFilter {

    private static final String CONFIG_SESSION_ENABLED = "repository.webdav.session.enabled";
    private static final String JSESSIONID = "JSESSIONID";
    public static final String COOKIE = "Cookie";
    public static final String SET_COOKIE = "Set-Cookie";


    @Setter(onMethod_ = @Autowired)
    private LightbendConfigLoader configLoader;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Config eduConfig = configLoader.getConfig();
        boolean sessionDisabled = false;
        if (eduConfig != null && eduConfig.hasPath(CONFIG_SESSION_ENABLED)) {
            sessionDisabled = !eduConfig.getBoolean(CONFIG_SESSION_ENABLED);
        }

        if (sessionDisabled && request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            httpRequest = new WebDAVSessionRequestWrapper(httpRequest);
            httpResponse = new WebDAVSessionResponseWrapper(httpResponse);

            chain.doFilter(httpRequest, httpResponse);
        } else {
            chain.doFilter(request, response);
        }
    }

    static class WebDAVSessionRequestWrapper extends HttpServletRequestWrapper {

        private HttpSession statelessSession;

        public WebDAVSessionRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public Cookie[] getCookies() {
            Cookie[] cookies = super.getCookies();
            if (cookies == null) {
                return null;
            }
            List<Cookie> filteredCookies = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (!JSESSIONID.equalsIgnoreCase(cookie.getName())) {
                    filteredCookies.add(cookie);
                }
            }
            return filteredCookies.isEmpty() ? null : filteredCookies.toArray(new Cookie[0]);
        }

        @Override
        public String getHeader(String name) {
            if (COOKIE.equalsIgnoreCase(name)) {
                return getFilteredCookieHeader();
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (COOKIE.equalsIgnoreCase(name)) {
                String filteredHeader = getFilteredCookieHeader();
                if (filteredHeader == null) {
                    return Collections.emptyEnumeration();
                }
                return Collections.enumeration(Collections.singletonList(filteredHeader));
            }
            return super.getHeaders(name);
        }

        private String getFilteredCookieHeader() {
            String cookieHeader = super.getHeader(COOKIE);
            if (cookieHeader == null) {
                return null;
            }
            String[] cookies = cookieHeader.split(";\\s*");
            List<String> filteredCookies = new ArrayList<>();
            for (String cookie : cookies) {
                if (!cookie.trim().toUpperCase().startsWith(JSESSIONID + "=")) {
                    filteredCookies.add(cookie);
                }
            }
            return filteredCookies.isEmpty() ? null : String.join("; ", filteredCookies);
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (!create) {
                return statelessSession;
            }
            if (statelessSession == null) {
                statelessSession = new StatelessHttpSession();
            }
            return statelessSession;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }
    }

    /**
     * A stateless HttpSession implementation that stores attributes only for the duration of the request.
     */
    private static class StatelessHttpSession implements HttpSession {

        private final Map<String, Object> attributes = new HashMap<>();
        private final long creationTime = System.currentTimeMillis();

        @Override
        public long getCreationTime() {
            return creationTime;
        }

        @Override
        public String getId() {
            return "stateless-webdav-session";
        }

        @Override
        public long getLastAccessedTime() {
            return creationTime;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        @Override
        public void setAttribute(String name, Object value) {
            if (value == null) {
                removeAttribute(name);
            } else {
                attributes.put(name, value);
            }
        }

        @Override
        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        @Override
        public void invalidate() {
            attributes.clear();
        }

        @Override
        public boolean isNew() {
            return true;
        }
    }

    static class WebDAVSessionResponseWrapper extends HttpServletResponseWrapper {
        public WebDAVSessionResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if (SET_COOKIE.equalsIgnoreCase(name) && value != null && value.toUpperCase().contains(JSESSIONID)) {
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (SET_COOKIE.equalsIgnoreCase(name) && value != null && value.toUpperCase().contains(JSESSIONID)) {
                return;
            }
            super.setHeader(name, value);
        }
    }
}
