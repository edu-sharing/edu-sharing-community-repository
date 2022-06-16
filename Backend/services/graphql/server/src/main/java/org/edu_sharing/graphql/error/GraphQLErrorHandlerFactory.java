package org.edu_sharing.graphql.error;

import graphql.kickstart.execution.error.DefaultGraphQLErrorHandler;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.edu_sharing.graphql.error.GraphQLErrorFactory.withReflection;

@Slf4j
public class GraphQLErrorHandlerFactory {

    public GraphQLErrorHandler create(
            ConfigurableApplicationContext applicationContext, boolean exceptionHandlersEnabled) {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        List<GraphQLErrorFactory> factories =
                Arrays.stream(beanFactory.getBeanDefinitionNames())
                        .filter(applicationContext::containsBean)
                        .map(name -> scanForExceptionHandlers(applicationContext, name))
                        .flatMap(List::stream)
                        .collect(toList());

        if (exceptionHandlersEnabled) {
            log.debug(
                    "Handle GraphQL errors using exception handlers defined in {} custom factories",
                    factories.size());
            return new GraphQLErrorFromExceptionHandler(factories);
        }

        log.debug("Using default GraphQL error handler");
        return new DefaultGraphQLErrorHandler();
    }

    private List<GraphQLErrorFactory> scanForExceptionHandlers(
            ApplicationContext context, String name) {
        try {
            Class<?> objClz = context.getType(name);
            if (objClz == null) {
                log.info("Cannot load class {}", name);
                return emptyList();
            }
            return Arrays.stream(objClz.getDeclaredMethods())
                    .map(method -> AopUtils.getMostSpecificMethod(method, objClz))
                    .filter(ReflectiveMethodValidator::isGraphQLExceptionHandler)
                    .map(method -> withReflection(context.getBean(name), method))
                    .collect(toList());
        } catch (BeanCreationException e) {
            log.error("Cannot load class {}. {}", name, e.getMessage());
            return emptyList();
        }
    }
}
