package org.edu_sharing.lightbend;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@RequiredArgsConstructor
public class AutoRefreshPropertyBeanProvider<T> {

    private final Class<T> typeInformation;
    private final LightbendConfigLoader lightbendConfigLoader;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final AtomicBoolean invalidated = new AtomicBoolean(true);

    private volatile T instance;

    public T getInstance() {
        if (invalidated.get()) {
            synchronized (this) {
                if (invalidated.get()) {
                    instance = createConfigurationPropertyBean();
                    invalidated.set(false);
                }
            }
        }
        return instance;
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onContextRefreshedEvent() {
        invalidated.set(true);
    }

    private T createConfigurationPropertyBean() {
        Config config = getConfig();
        if (config != null) {
            return ConfigBeanFactory.create(config, typeInformation);
        } else {
            return BeanUtils.instantiateClass(typeInformation);
        }
    }

    private Config getConfig() {
        String prefix = getPrefix(typeInformation);
        try {
            Expression expression = parser.parseExpression(prefix, ParserContext.TEMPLATE_EXPRESSION);
            EvaluationContext context = new StandardEvaluationContext();
            String configPath = expression.getValue(context, String.class);
            return lightbendConfigLoader.getConfig().getConfig(configPath);
        } catch (ConfigException e) {
            log.warn("Could not find config path '{}' for type {}: {}", prefix, typeInformation.getName(), e.getMessage());
            return null;
        }
    }

    private String getPrefix(final Class<?> beanClazz) {
        ConfigurationProperties configurationProperties = beanClazz.getAnnotation(ConfigurationProperties.class);
        if (configurationProperties != null) {
            return configurationProperties.prefix();
        }
        throw new NotImplementedException("Missing implementation for ConfigurationProperties annotation of class: " + beanClazz.getName());
    }

}
