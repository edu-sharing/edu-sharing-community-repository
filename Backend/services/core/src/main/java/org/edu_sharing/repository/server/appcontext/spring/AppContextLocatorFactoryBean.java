package org.edu_sharing.repository.server.appcontext.spring;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;
import org.edu_sharing.repository.server.appcontext.SimpleAppContextServiceFactoryImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Proxy;

@RequiredArgsConstructor
public class AppContextLocatorFactoryBean<I, S> implements FactoryBean<I> {

    private final Class<I> interfaceType;
    private final Class<S> serviceType;
    private final ApplicationContext applicationContext;


    @Override
    public I getObject() {
        AppContextServiceLocator locator = applicationContext.getBean(AppContextServiceLocator.class);
        SimpleAppContextServiceFactoryImpl<S> instance = new SimpleAppContextServiceFactoryImpl<>(locator, serviceType);

        //noinspection unchecked
        return (I)Proxy.newProxyInstance(interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                (p, method, args) -> method.invoke(instance, args));
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }
}
