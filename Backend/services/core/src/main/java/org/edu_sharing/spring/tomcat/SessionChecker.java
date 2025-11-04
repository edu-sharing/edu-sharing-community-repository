package org.edu_sharing.spring.tomcat;

import jakarta.servlet.ServletContext;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;

import org.apache.catalina.loader.WebappClassLoaderBase;
import org.edu_sharing.alfresco.monitoring.TomcatUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SessionChecker {

    private final ServletContext servletContext;

    public SessionChecker(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public boolean isSessionAlive(String sessionId) throws IOException {
        StandardContext stdContext = new TomcatUtil().getContext(servletContext.getContextPath());
        Manager manager = stdContext.getManager();
        Session session = manager.findSession(sessionId);
        return session != null && session.isValid();
    }
}

