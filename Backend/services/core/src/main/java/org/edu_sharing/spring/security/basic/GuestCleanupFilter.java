package org.edu_sharing.spring.security.basic;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GuestCleanupFilter extends OncePerRequestFilter {

    private final GuestService guestService;

    public GuestCleanupFilter(GuestService guestService) {
        this.guestService = guestService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Filter only OAuth2 authorize requests
        if (request.getServletPath().startsWith("/oauth2server") || request.getServletPath().startsWith("/shibboleth")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth != null && auth.getPrincipal() instanceof EduSharingPrincipal u && guestService.isGuestUser(u.getUsername())){
                SecurityContextHolder.getContext().setAuthentication(null);
                request.getSession().removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }
        }

        filterChain.doFilter(request, response);
    }
}
