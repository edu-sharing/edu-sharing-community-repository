package org.edu_sharing.graphql.web;


import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.GraphQLSchema;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

public class GraphQLServlet extends GraphQLHttpServlet {
    @Autowired private GraphQLConfiguration graphQLConfiguration;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        springBeanAutowiringProcessInjectionBasedOnEduSharingContext();
    }

    /**
     * Registers the servlet with the spring bean autowireing service.
     *
     * Note: Usually we could do this by calling:
     * SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
     * Unfortunately we are in a servlet instantiated by alfresco,
     * therefore we get the ioc container from alfresco, not from edu-sharing
     */
    private void springBeanAutowiringProcessInjectionBasedOnEduSharingContext() {
        // returns application context of edu sharing
        ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();

        // this is what SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this) does internally
        AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
        bpp.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
        bpp.processInjection(this);
    }

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return graphQLConfiguration;
    }
}