package org.edu_sharing.repository.server.authentication;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Keeps the {@link LoginRedirect#PARAM} of an SSO start in the session. Mapped to the
 * SSO entry points ({@code /shibboleth}, {@code /sso}) <b>before</b> the spring security
 * chain, which would otherwise send the browser to the IdP first.
 */
public class LoginRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LoginRedirect.remember((HttpServletRequest) request);
        chain.doFilter(request, response);
    }
}
