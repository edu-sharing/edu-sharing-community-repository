package org.edu_sharing.graphql.web;

import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.kickstart.execution.BatchedDataLoaderGraphQLBuilder;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.config.GraphQLBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class GraphQLInvokerAutoConfiguration {
    @Bean
    public GraphQLInvoker graphQLInvoker(
            GraphQLBuilder graphQLBuilder,
            BatchedDataLoaderGraphQLBuilder batchedDataLoaderGraphQLBuilder){
        return new GraphQLInvoker(graphQLBuilder, batchedDataLoaderGraphQLBuilder);
    }

    @Autowired(required = false)
    Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier;
    @Bean
    public BatchedDataLoaderGraphQLBuilder batchedDataLoaderGraphQLBuilder(){
        return  new BatchedDataLoaderGraphQLBuilder(optionsSupplier);
    }

}
