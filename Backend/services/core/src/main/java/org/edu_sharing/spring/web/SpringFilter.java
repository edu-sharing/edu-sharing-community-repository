package org.edu_sharing.spring.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * An abstract base class for servlet filters that supports autowiring of Spring beans.
 * <p>
 * The {@code SpringFilter} simplifies the integration of Spring-managed beans into
 * Java servlet filters by automatically autowiring them during the filter initialization phase.
 * This is achieved by leveraging the {@code WebApplicationContextUtils} class to access the
 * Spring application context and configure the filter instance.
 * <p>
 * Subclasses of {@code SpringFilter} are expected to implement the standard methods of the
 * {@code jakarta.servlet.Filter} interface, such as {@code doFilter}, to define custom filter logic.
 * <p>
 * The autowiring mechanism ensures that dependencies declared within the subclass are injected,
 * enabling seamless use of Spring-managed components within the filter's operation.
 * <p>
 * Usage:
 * Subclasses should extend the {@code SpringFilter} and implement the required filter methods.
 * During the filter's initialization phase, Spring beans will be autowired into the subclass
 * automatically.
 */
public abstract class SpringFilter implements Filter {

    /**
     * Initializes the filter and autowires Spring-managed beans into the filter instance.
     * This method is invoked by the servlet container during the filter initialization phase.
     *
     * @param filterConfig the configuration object provided by the servlet container
     *                      containing initialization parameters and a reference to the
     *                      {@code ServletContext}.
     * @throws ServletException if an exception occurs that interferes with the filter's
     *                          initialization.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext())
                .getAutowireCapableBeanFactory()
                .autowireBean(this);
    }
}
