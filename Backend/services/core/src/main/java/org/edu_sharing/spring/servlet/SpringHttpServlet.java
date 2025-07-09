package org.edu_sharing.spring.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * Abstract base class for HTTP servlets that integrates with the Spring framework.
 *
 * This class extends {@code HttpServlet} and enables Spring dependency injection
 * for subclasses by leveraging the {@code SpringBeanAutowiringSupport} utility.
 * It is designed to provide a foundation for servlets that require Spring-managed
 * beans and automatic initialization within their lifecycle.
 *
 * Subclasses must implement required methods from {@code HttpServlet}, such as
 * {@code doGet(HttpServletRequest, HttpServletResponse)} or
 * {@code doPost(HttpServletRequest, HttpServletResponse)}, to define the specific
 * behavior of the servlet. By inheriting from this class, Spring beans can be
 * automatically injected into the subclass upon servlet initialization.
 *
 * This class overrides the {@code init()} method to include logic for processing
 * Spring dependency injection in the servlet's context. Subclasses should call
 * {@code super.init()} if they override the {@code init()} method to ensure
 * proper initialization of Spring dependencies.
 *
 * Note: This class is abstract and cannot be instantiated directly.
 */
public abstract class SpringHttpServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }
}
