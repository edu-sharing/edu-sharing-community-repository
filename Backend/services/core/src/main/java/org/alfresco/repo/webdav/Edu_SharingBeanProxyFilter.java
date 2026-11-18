package org.alfresco.repo.webdav;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.repo.webdav.auth.BaseAuthenticationFilter;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.io.IOException;

@Slf4j
public class Edu_SharingBeanProxyFilter implements Filter
{
    /**
     * Name of the init parameter that carries the proxied bean name 
     */
    private static final String INIT_PARAM_BEAN_NAME = "beanName";
    
    private DependencyInjectedFilter filter;
    private ServletContext context;

    Config eduConfig = LightbendConfigLoader.get();

    static String CONFIG_ENABLED = "repository.webdav.enabled";

    boolean enabled = true;
    
    /**
     * Initialize the filter.
     * 
     * @param args
     *            FilterConfig
     * @throws ServletException
     *             the servlet exception
     * @exception ServletException
     */
    public void init(FilterConfig args) throws ServletException
    {
        if(eduConfig.hasPath(CONFIG_ENABLED)){
            enabled = eduConfig.getBoolean(CONFIG_ENABLED);
        }
        this.context = args.getServletContext();
    	this.filter = (DependencyInjectedFilter) ApplicationContextFactory.getApplicationContext().getBean(args.getInitParameter(INIT_PARAM_BEAN_NAME));
        if(this.filter instanceof Filter){
            ((Filter)this.filter).init(args);
        }
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#destroy()
     */
    public void destroy()
    {
        this.filter = null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException
    {
        if(enabled) this.filter.doFilter(this.context, request, response, twoFactorGuard(chain));
    }

    /**
     * Wraps the chain so that, once the proxied authentication filter (alfresco, ldap, idm-rest, shibboleth, ...)
     * has authenticated the user via basic auth, ticket or an existing session, WebDAV access is blocked for users
     * with active 2FA. WebDAV clients can't provide a second factor, so the password alone must not be sufficient.
     */
    static FilterChain twoFactorGuard(FilterChain chain) {
        return (request, response) -> {
            String username = AuthenticationUtil.getFullyAuthenticatedUser();
            if (username == null || ApplicationInfoList.isInternalPortRequest(request) || !is2FaActive(username)) {
                chain.doFilter(request, response);
                return;
            }
            log.info("blocked webdav access for user with active 2fa");
            HttpSession session = ((HttpServletRequest) request).getSession(false);
            if (session != null) {
                session.removeAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);
                session.invalidate();
            }
            AuthenticationUtil.clearCurrentSecurityContext();

            // 403 instead of 401: a new password prompt would not help, the client can't provide the second factor
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResp.setContentType("text/plain;charset=UTF-8");
            httpResp.getWriter().print("WebDAV access is not available for accounts with two-factor authentication enabled");
            httpResp.flushBuffer();
        };
    }

    private static boolean is2FaActive(String username) {
        try {
            return AuthorityServiceFactory.getInstance().getLocalService().is2FaActive(username);
        } catch (Exception e) {
            // fail closed: if the status can't be determined, don't allow password-only access
            log.error("could not determine 2fa status, blocking webdav access", e);
            return true;
        }
    }

}
