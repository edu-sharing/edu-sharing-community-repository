package org.edu_sharing.spring.security.server.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.RepositoryAuthenticatedUser;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GuestBlockingFilter extends OncePerRequestFilter {

    private final GuestService guestService;

    public GuestBlockingFilter(GuestService guestService) {
        this.guestService = guestService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Filter only OAuth2 authorize requests
        if (uri.contains("/oauth2server/authorize")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth != null && auth.getPrincipal() instanceof RepositoryAuthenticatedUser u && guestService.isGuestUser(u.getUsername())){
                SecurityContextHolder.getContext().setAuthentication(null);
            }
        }

        filterChain.doFilter(request, response);
    }
}
