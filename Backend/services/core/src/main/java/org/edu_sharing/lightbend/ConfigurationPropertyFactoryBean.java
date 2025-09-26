package org.edu_sharing.lightbend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.ApplicationContext;

@Slf4j
@RequiredArgsConstructor
public class ConfigurationPropertyFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> typeInformation;
    private final ApplicationContext applicationContext;

    @Override
    public T getObject() {
        AutoRefreshPropertyBeanProvider<T> created = new AutoRefreshPropertyBeanProvider<>(typeInformation, applicationContext.getBean(LightbendConfigLoader.class));
        String beanName = "autoRefreshPropertyBeanProvider-" + typeInformation.getName();
        ConfigurableListableBeanFactory autowireCapableBeanFactory = (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.initializeBean(created, beanName);
        autowireCapableBeanFactory.autowireBean(created);
        autowireCapableBeanFactory.registerSingleton(beanName, created);

        //noinspection unchecked
        return (T) Enhancer.create(typeInformation, (MethodInterceptor) (obj, method, args, proxy) -> proxy.invoke(created.getInstance(), args));
    }


    @Override
    public Class<?> getObjectType() {
        return typeInformation;
    }
}
