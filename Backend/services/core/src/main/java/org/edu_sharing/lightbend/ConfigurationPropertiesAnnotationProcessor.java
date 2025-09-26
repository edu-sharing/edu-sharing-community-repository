package org.edu_sharing.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Slf4j
@Component
public class ConfigurationPropertiesAnnotationProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, PriorityOrdered {

    private final AnnotationBeanNameGenerator annotationBeanNameGenerator = new AnnotationBeanNameGenerator();

    @Setter
    private ApplicationContext applicationContext;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }


    @SneakyThrows
    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry registry) throws BeansException {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents("org.edu_sharing")) {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            if (clazz.isInterface()) {
                throw new BeanInstantiationException(clazz, "Specified class is an interface");
            }

            String[] beanNames = applicationContext.getBeanNamesForType(clazz);
            BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(ConfigurationPropertyFactoryBean.class, () -> new ConfigurationPropertyFactoryBean<>(clazz, applicationContext));
            bdb.setPrimary(true);
            bdb.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            String beanName;
            if (beanNames.length > 1) {
                beanName = Arrays.stream(beanNames)
                        .filter(x -> registry.getBeanDefinition(x) instanceof RootBeanDefinition)
                        .findFirst()
                        .orElseThrow(() -> new BeanInstantiationException(clazz, "Could not resolve bean definition for " + clazz.getName()));
            } else if (beanNames.length == 1) {
                beanName = beanNames[0];
            } else {
                beanName = annotationBeanNameGenerator.generateBeanName(BeanDefinitionBuilder.genericBeanDefinition(clazz).getBeanDefinition(), registry);
            }

            registry.registerBeanDefinition(beanName, bdb.getBeanDefinition());

        }
    }
}
