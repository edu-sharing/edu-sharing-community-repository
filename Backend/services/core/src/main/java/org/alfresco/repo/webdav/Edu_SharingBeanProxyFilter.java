package org.alfresco.repo.webdav;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.typesafe.config.Config;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.repo.webdav.auth.LDAPAuthenticationFilter;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.spring.ApplicationContextFactory;

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
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
        this.filter = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException
    {
        if(enabled) this.filter.doFilter(this.context, request, response, chain);
    }

}
