package org.edu_sharing.spring.security.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class SecurityContextStrategySwitchFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("Switching SecurityContextHolderStrategy to ThreadLocal for {}", path);
        DelegatingSecurityContextHolderStrategy.useThreadLocalStrategyForCurrentThread();

        try {
            filterChain.doFilter(request, response);
        } finally {
            DelegatingSecurityContextHolderStrategy.clearStrategyForCurrentThread();
        }
    }
}
