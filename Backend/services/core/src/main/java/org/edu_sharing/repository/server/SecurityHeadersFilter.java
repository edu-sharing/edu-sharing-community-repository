package org.edu_sharing.repository.server;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.io.IOException;

public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        HttpServletResponse res = (HttpServletResponse)servletResponse;
        if(HttpMethod.GET.equals(req.getMethod())){
            addResponseHeaders(res);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void addResponseHeaders(HttpServletResponse resp) {
        Config headers = LightbendConfigLoader.get().getConfig("angular.headers");
        resp.setHeader("X-XSS-Protection", headers.getString("X-XSS-Protection"));
        resp.setHeader("X-Frame-Options", headers.getString("X-Frame-Options"));
        Config securityConfigs = headers.getConfig("Content-Security-Policy");
        StringBuilder joined = new StringBuilder();
        securityConfigs.entrySet().forEach((e) ->
                joined.append(e.getKey()).append(" ").append(e.getValue().unwrapped().toString()).append("; ")
        );
        resp.setHeader("Content-Security-Policy", joined.toString());
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}
