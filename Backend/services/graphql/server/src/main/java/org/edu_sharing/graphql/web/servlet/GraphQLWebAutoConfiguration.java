package org.edu_sharing.graphql.web.servlet;

import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.config.*;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import graphql.kickstart.servlet.AsyncTaskDecorator;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.cache.GraphQLResponseCacheManager;
import graphql.kickstart.servlet.config.DefaultGraphQLSchemaServletProvider;
import graphql.kickstart.servlet.config.GraphQLSchemaServletProvider;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.kickstart.servlet.core.GraphQLServletRootObjectBuilder;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.schema.GraphQLSchema;
import org.edu_sharing.graphql.error.ErrorHandlerSupplier;
import org.edu_sharing.graphql.error.GraphQLErrorStartupListener;
import org.edu_sharing.graphql.web.servlet.metrics.MetricsInstrumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static graphql.kickstart.execution.GraphQLObjectMapper.newBuilder;

@Configuration
public class GraphQLWebAutoConfiguration {

    public static final String QUERY_EXECUTION_STRATEGY = "queryExecutionStrategy";
    public static final String MUTATION_EXECUTION_STRATEGY = "mutationExecutionStrategy";
    public static final String SUBSCRIPTION_EXECUTION_STRATEGY = "subscriptionExecutionStrategy";

    @Autowired GraphQLServletProperties graphQLServletProperties;
    @Autowired AsyncServletProperties asyncServletProperties;

    private final ErrorHandlerSupplier errorHandlerSupplier = new ErrorHandlerSupplier(null);



    @Autowired(required = false) GraphQLErrorHandler errorHandler;

    @Bean
    public GraphQLErrorStartupListener graphQLErrorStartupListener() {
        errorHandlerSupplier.setErrorHandler(errorHandler);
        return new GraphQLErrorStartupListener(
                errorHandlerSupplier, graphQLServletProperties.isExceptionHandlersEnabled());
    }

    @Bean
    public GraphQLSchemaServletProvider graphQLSchemaProvider(GraphQLSchema schema) {
        return new DefaultGraphQLSchemaServletProvider(schema);
    }

    @Autowired(required = false) Map<String, ExecutionStrategy> executionStrategies;

    @Bean
    public ExecutionStrategyProvider executionStrategyProvider(){
        if (executionStrategies == null || executionStrategies.isEmpty()) {
            return new DefaultExecutionStrategyProvider(
                    new AsyncExecutionStrategy(), null, new SubscriptionExecutionStrategy());
        } else if (executionStrategies.entrySet().size() == 1) {
            return new DefaultExecutionStrategyProvider(
                    executionStrategies.entrySet().stream()
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElseThrow(IllegalStateException::new));
        } else {

            if (!executionStrategies.containsKey(QUERY_EXECUTION_STRATEGY)) {
                throwIncorrectExecutionStrategyNameException();
            }

            if (executionStrategies.size() == 2
                    && !(executionStrategies.containsKey(MUTATION_EXECUTION_STRATEGY)
                    || executionStrategies.containsKey(SUBSCRIPTION_EXECUTION_STRATEGY))) {
                throwIncorrectExecutionStrategyNameException();
            }

            if (executionStrategies.size() >= 3
                    && !(executionStrategies.containsKey(MUTATION_EXECUTION_STRATEGY)
                    && executionStrategies.containsKey(SUBSCRIPTION_EXECUTION_STRATEGY))) {
                throwIncorrectExecutionStrategyNameException();
            }

            return new DefaultExecutionStrategyProvider(
                    executionStrategies.get(QUERY_EXECUTION_STRATEGY),
                    executionStrategies.get(MUTATION_EXECUTION_STRATEGY),
                    executionStrategies.get(SUBSCRIPTION_EXECUTION_STRATEGY));
        }
    }

    private void throwIncorrectExecutionStrategyNameException() {
        throw new IllegalStateException(
                String.format(
                        "When defining more than one execution strategy, they must be named %s, %s, or %s",
                        QUERY_EXECUTION_STRATEGY,
                        MUTATION_EXECUTION_STRATEGY,
                        SUBSCRIPTION_EXECUTION_STRATEGY));
    }



    @Autowired(required = false) GraphQLServletContextBuilder contextBuilder;
    @Autowired(required = false) GraphQLServletRootObjectBuilder graphQLRootObjectBuilder;

    @Bean
    public GraphQLInvocationInputFactory invocationInputFactory(GraphQLSchemaServletProvider schemaProvider) {
        GraphQLInvocationInputFactory.Builder builder = GraphQLInvocationInputFactory.newBuilder(schemaProvider);

        Optional.ofNullable(graphQLRootObjectBuilder).ifPresent(builder::withGraphQLRootObjectBuilder);
        Optional.ofNullable(contextBuilder).ifPresent(builder::withGraphQLContextBuilder);

        return builder.build();
    }

    @Autowired(required = false) List<Instrumentation> instrumentations;
    @Autowired(required = false) PreparsedDocumentProvider preparsedDocumentProvider;
    @Autowired(required = false) GraphQLBuilderConfigurer graphQLBuilderConfigurer;

    @Bean
    public GraphQLBuilder graphQLBuilder(ExecutionStrategyProvider executionStrategyProvider) {
        GraphQLBuilder graphQLBuilder = new GraphQLBuilder();
        graphQLBuilder.executionStrategyProvider(() -> executionStrategyProvider);

        if (instrumentations != null && !instrumentations.isEmpty()) {
            if (instrumentations.size() == 1) {
                graphQLBuilder.instrumentation(() -> instrumentations.get(0));
            } else {
                // Metrics instrumentation should be the last to run (we need that from
                // TracingInstrumentation)
                instrumentations.sort((a, b) -> a instanceof MetricsInstrumentation ? 1 : 0);
                graphQLBuilder.instrumentation(() -> new ChainedInstrumentation(instrumentations));
            }
        }

        if (preparsedDocumentProvider != null) {
            graphQLBuilder.preparsedDocumentProvider(() -> preparsedDocumentProvider);
        }

        if (graphQLBuilderConfigurer != null) {
            graphQLBuilder.graphQLBuilderConfigurer(() -> graphQLBuilderConfigurer);
        }

        return graphQLBuilder;
    }


    @Autowired(required = false) ObjectMapperProvider objectMapperProvider;
    @Autowired(required = false) GraphQLServletObjectMapperConfigurer objectMapperConfigurer;

    @Bean
    public GraphQLObjectMapper graphQLObjectMapper() {
        GraphQLObjectMapper.Builder builder = newBuilder();
        builder.withGraphQLErrorHandler(errorHandlerSupplier);

        if (objectMapperProvider != null) {
            builder.withObjectMapperProvider(objectMapperProvider);
        } else if (objectMapperConfigurer != null) {
            builder.withObjectMapperConfigurer(objectMapperConfigurer);
        }
        return builder.build();
    }

    @Autowired(required = false) List<GraphQLServletListener> listeners;
    @Autowired(required = false) BatchInputPreProcessor batchInputPreProcessor;
    @Autowired(required = false) GraphQLResponseCacheManager responseCacheManager;
    @Autowired(required = false) AsyncTaskDecorator asyncTaskDecorator;
    @Autowired(required = false) @Qualifier("graphqlAsyncTaskExecutor") Executor asyncExecutor;

    @Bean
    public GraphQLConfiguration graphQLServletConfiguration(
            GraphQLInvocationInputFactory invocationInputFactory,
            GraphQLInvoker graphQLInvoker,
            GraphQLObjectMapper graphQLObjectMapper) {
        long asyncTimeoutMilliseconds = Optional.ofNullable(asyncServletProperties.getTimeout()) //
                .orElse(AsyncServletProperties.DEFAULT_TIMEOUT).toMillis();

        long subscriptionTimeoutMilliseconds = Optional.ofNullable(graphQLServletProperties.getSubscriptionTimeout()) //
                .orElse(GraphQLServletProperties.DEFAULT_SUBSCRIPTION_TIMEOUT).toMillis();

        return GraphQLConfiguration
                .with(invocationInputFactory)
                .with(graphQLInvoker)
                .with(graphQLObjectMapper)
                .with(listeners)
                .with(subscriptionTimeoutMilliseconds)
                .with(batchInputPreProcessor)
                .with(graphQLServletProperties.getContextSetting())
                .with(responseCacheManager)
                .asyncTimeout(asyncTimeoutMilliseconds)
                .with(asyncTaskDecorator)
                .asyncCorePoolSize(asyncServletProperties.getThreads().getMin())
                .asyncMaxPoolSize(asyncServletProperties.getThreads().getMax())
                .with(asyncExecutor)
                .build();
    }
}
