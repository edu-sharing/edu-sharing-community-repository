package org.edu_sharing.repository.server.appcontext.spring;

import lombok.Setter;
import org.edu_sharing.repository.server.appcontext.AppContextRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AppContextRegistryBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, PriorityOrdered {

    @Setter
    private ApplicationContext applicationContext;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        AppContextRegistry contextRegistry = applicationContext.getBean(AppContextRegistry.class);
        Set<Class<?>> managed = contextRegistry.getContexts().values().stream()
                .map(AppContextRegistry.ContextDefinition::overrides)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        for (Class<?> type : managed) {
            validateBeanDefinitionExists(type);
        }

        for (AppContextRegistry.ContextDefinition def : contextRegistry.getContexts().values()) {
            Collection<AppContextRegistry.BeanOverride<?>> values = def.overrides().values();
            for (AppContextRegistry.BeanOverride<?> value : values) {
                value.validateBeanDefinitionExists(applicationContext);
            }
        }


    }

    private void validateBeanDefinitionExists(Class<?> type) {
        String[] beanNames = applicationContext.getBeanNamesForType(type);
        if (beanNames.length == 0) {
            throw new IllegalStateException("No bean definition found for type: " + type.getName());
        }
    }
}
