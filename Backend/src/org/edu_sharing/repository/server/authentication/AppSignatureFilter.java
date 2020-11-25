package org.edu_sharing.repository.server.authentication;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AppSignatureFilter implements javax.servlet.Filter {

    Logger logger = Logger.getLogger(AppSignatureFilter.class);

    static ThreadLocal<ApplicationInfo> appInfoThreadLocal = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            ApplicationInfo appInfo = new SignatureVerifier().verifyAppSignature((HttpServletRequest) servletRequest);
            HttpServletResponse resp = (HttpServletResponse) servletResponse;
            if (appInfo == null) {
                String message = "application request could not be verified";
                logger.error(message);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
                return;
            } else {
                appInfoThreadLocal.set(appInfo);
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }finally {
            appInfoThreadLocal.set(null);
        }
    }

    public static ApplicationInfo getAppInfo(){
        return appInfoThreadLocal.get();
    }

    @Override
    public void destroy() {

    }
}
