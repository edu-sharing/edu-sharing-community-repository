package org.edu_sharing.repository.server.appcontext.spring;


import freemarker.template.utility.ClassUtil;
import lombok.Setter;
import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AppContextLocatorAutoRegistrar implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, PriorityOrdered {

    @Setter
    private ApplicationContext applicationContext;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<AppContextLocatorTypeInformation> typeInformations = scanLocatorInterfaces(registry);
        for (AppContextLocatorTypeInformation typeInformation : typeInformations) {


            String beanName = typeInformation.interfaceType().getName();
            if (registry.containsBeanDefinition(beanName)) {
                continue;
            }

            BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(AppContextLocatorFactoryBean.class, () ->
                    new AppContextLocatorFactoryBean<>(typeInformation.interfaceType(), typeInformation.serviceType(), applicationContext));

            bdb.setPrimary(true);
            bdb.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            registry.registerBeanDefinition(beanName, bdb.getBeanDefinition());


        }
    }

    private Set<AppContextLocatorTypeInformation> scanLocatorInterfaces(BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = new AppContextLocatorComponentProvider(registry);
        Set<AppContextLocatorTypeInformation> result = new HashSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("org.edu_sharing")) {
            try {
                Class<?> iface = ClassUtil.forName(bd.getBeanClassName());
                // Nur Interfaces berücksichtigen, die AppContextLocator<T> direkt/indirekt erweitern
                ResolvableType rt = ResolvableType.forClass(iface);
                Class<?> generic = resolveLocatorGeneric(rt);
                if (generic != null) result.add(new AppContextLocatorTypeInformation(iface, generic));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return result;
    }

    private record AppContextLocatorTypeInformation<I extends AppContextServiceFactory<S>, S>(Class<I> interfaceType, Class<S> serviceType) {
    }

    private Class<?> resolveLocatorGeneric(ResolvableType type) {
        // Direktes Match?
        for (ResolvableType iface : type.getInterfaces()) {
            if (iface.resolve() == AppContextServiceFactory.class) {
                return iface.getGeneric(0).resolve();
            }
        }
        // Rekursiv über Ober-Interfaces
        for (ResolvableType iface : type.getInterfaces()) {
            Class<?> g = resolveLocatorGeneric(iface);
            if (g != null) {
                return g;
            }
        }
        return null;
    }
}
