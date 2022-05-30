package org.edu_sharing.repository.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;

public class DisableCacheFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Expires", "0");
        resp.setHeader("Cache-Control", "no-cache");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

}