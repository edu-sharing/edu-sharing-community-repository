package org.edu_sharing.repository.server.appcontext.spring;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;
import org.edu_sharing.repository.server.appcontext.ContextAwareProxyFactory;
import org.edu_sharing.repository.server.appcontext.UseAppContext;
import org.edu_sharing.repository.server.appcontext.UseLocalAppContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
public class AppContextAwareProxyFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> type;
    private final ApplicationContext applicationContext;

    @Autowired(required = false)
    private InjectionPoint injectionPoint;


    @Override
    public T getObject() {
        AppContextServiceLocator locator = applicationContext.getBean(AppContextServiceLocator.class);
        UseAppContext ann = injectionPoint != null
                ? injectionPoint.getAnnotatedElement().getAnnotation(UseAppContext.class)
                : null;

        if(ann != null){
            return locator.get(type, ann.value());
        }

        UseLocalAppContext localAppContext = injectionPoint != null
                ? injectionPoint.getAnnotatedElement().getAnnotation(UseLocalAppContext.class)
                : null;

        if(localAppContext != null){
            return locator.getLocal(type);
        }

        return ContextAwareProxyFactory.create(type, locator);
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }
}
