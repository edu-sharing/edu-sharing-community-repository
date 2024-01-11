package org.alfresco.repo.webdav;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

public class Edu_SharingBeanProxyFilter implements Filter
{
    /**
     * Name of the init parameter that carries the proxied bean name 
     */
    private static final String INIT_PARAM_BEAN_NAME = "beanName";
    
    private DependencyInjectedFilter filter;
    private ServletContext context;    
    
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
    	this.filter = (DependencyInjectedFilter)AlfAppContextGate.getApplicationContext().getBean(args.getInitParameter(INIT_PARAM_BEAN_NAME));
        this.context = args.getServletContext();
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
        this.filter.doFilter(this.context, request, response, chain);
    }

}
