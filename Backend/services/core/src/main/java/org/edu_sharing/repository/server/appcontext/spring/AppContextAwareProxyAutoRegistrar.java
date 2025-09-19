package org.edu_sharing.repository.server.appcontext.spring;

import lombok.Setter;
import org.edu_sharing.repository.server.appcontext.*;
import org.springframework.beans.BeansException;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

@Component
public class AppContextAwareProxyAutoRegistrar implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, PriorityOrdered {

    @Setter
    private ApplicationContext applicationContext;


    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        AppContextRegistry contextRegistry = applicationContext.getBean(AppContextRegistry.class);

        // 1) collect interfaces from AppContextRegistry-overrides
        Set<Class<?>> managed = new HashSet<>(collectFromContextRegistry(contextRegistry));

        // 2) collect Interfaces from @AppContextManaged-annotated beans
        managed.addAll(collectFromAnnotatedBeans());

        // 3) for each interface, register a primary proxy bean definition (if not already registered)
        for (Class<?> type : managed) {
            if (!type.isInterface()) continue;

            String proxyBeanName = proxyBeanNameFor(type);
            if (registry.containsBeanDefinition(proxyBeanName)) {
                continue;
            }

            // register BeanDefinition for FactoryBean
            BeanDefinitionBuilder bdb = BeanDefinitionBuilder
                    .genericBeanDefinition(AppContextAwareProxyFactoryBean.class,
                            () -> new AppContextAwareProxyFactoryBean<>(type, applicationContext));

            // Set primary flag on bean definition
            bdb.setPrimary(true);

            // export as autowire candidate
            bdb.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

            registry.registerBeanDefinition(proxyBeanName, bdb.getBeanDefinition());
        }
    }

    private static String proxyBeanNameFor(Class<?> type) {
        return "current" + type.getName();
    }

    private static Set<Class<?>> collectFromContextRegistry(AppContextRegistry registry) {
        Set<Class<?>> types = new HashSet<>();
        for (AppContextRegistry.ContextDefinition def : registry.getContexts().values()) {
            types.addAll(def.overrides().keySet());
        }
        return types;
    }


    private Set<Class<?>> collectFromAnnotatedBeans() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AppContextManaged.class, true, true));
        scanner.addIncludeFilter(new AnnotationTypeFilter(LocalAppContext.class, true, true));
        scanner.addIncludeFilter(new AnnotationTypeFilter(AppContext.class, true, true));

        Set<Class<?>> managedInterfaces = new HashSet<>();
        ClassLoader cl = applicationContext.getClassLoader();

        for (var bd : scanner.findCandidateComponents("org.edu_sharing")) {
            try {
                Assert.notNull(bd.getBeanClassName(), "Bean class name must not be null");
                Class<?> clazz = ClassUtils.forName(bd.getBeanClassName(), cl);
                if (clazz.isInterface() && clazz.isAnnotationPresent(AppContextManaged.class)) {
                    managedInterfaces.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Konnte Klasse nicht laden: " + bd.getBeanClassName(), e);
            }
        }

        return managedInterfaces;
    }


}
