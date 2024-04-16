package org.edu_sharing.graphql.tools;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.kickstart.tools.*;
import graphql.kickstart.tools.proxy.ProxyHandler;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.visibility.GraphqlFieldVisibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
public class GraphQLJavaToolsAutoConfiguation {

    @Bean
    ObjectMapperConfigurer objectMapperConfigurer() {
        return ((mapper, context) -> {
            mapper.registerModule(new JavaTimeModule());
        });
    }


    @Bean
    public SchemaStringProvider schemaStringProvider(ApplicationContext applicationContext){
        // only fetch graphql files from the backend classpath and ignore Frontend graphql files
        return new ClasspathResourceSchemaStringProvider("**/edu_sharing/**/*.graphql");
    }

    @Autowired(required = false) PerFieldObjectMapperProvider perFieldObjectMapperProvider;
    @Autowired(required = false) List<SchemaParserOptions.GenericWrapper> genericWrappers;
    @Autowired(required = false) ObjectMapperConfigurer objectMapperConfigurer;
    @Autowired(required = false) List<ProxyHandler> proxyHandlers;
    @Autowired(required = false) CoroutineContextProvider coroutineContextProvider;
    @Autowired(required = false) List<TypeDefinitionFactory> typeDefinitionFactories;
    @Autowired(required = false) GraphqlFieldVisibility fieldVisibility;

    @Bean
    public SchemaParserOptions.Builder optionsBuilder() {

        SchemaParserOptions.Builder optionsBuilder = SchemaParserOptions.newOptions();

        if(perFieldObjectMapperProvider != null) {
            optionsBuilder.objectMapperProvider(perFieldObjectMapperProvider);
        }else{
            Optional.ofNullable(objectMapperConfigurer).ifPresent(optionsBuilder::objectMapperConfigurer);
        }

        Optional.ofNullable(genericWrappers).ifPresent(optionsBuilder::genericWrappers);
        Optional.ofNullable(proxyHandlers).ifPresent(handler->handler.forEach(optionsBuilder::addProxyHandler));
        Optional.ofNullable(coroutineContextProvider).ifPresent(optionsBuilder::coroutineContextProvider);
        Optional.ofNullable(typeDefinitionFactories).ifPresent(typeDefFac->typeDefFac.forEach(optionsBuilder::typeDefinitionFactory));
        Optional.ofNullable(fieldVisibility).ifPresent(optionsBuilder::fieldVisibility);

        return optionsBuilder;
    }


    @Autowired(required = false) List<SchemaParserDictionary> dictionarySet;
    @Autowired(required = false) List<GraphQLScalarType> scalars;
    @Autowired(required = false) List<SchemaDirective> directives;
    @Autowired(required = false) List<SchemaDirectiveWiring> directiveWirings;

    @Bean
    public SchemaParser schemaParser(
            List<GraphQLResolver<?>> resolvers,
            SchemaStringProvider schemaStringProvider,
            SchemaParserOptions.Builder optionsBuilder) throws IOException {

        SchemaParserBuilder builder = new SchemaParserBuilder();

        List<String> schemaStrings = schemaStringProvider.schemaStrings();
        schemaStrings.forEach(builder::schemaString);
        builder.options(optionsBuilder.build());
        Optional.ofNullable(scalars).ifPresent(builder::scalars);
        Optional.ofNullable(dictionarySet).ifPresent(dictionary -> dictionary.forEach(schemaParserDictionary -> builder.dictionary(schemaParserDictionary.getDictionary())));
        Optional.ofNullable(directives).ifPresent(directive->directive.forEach(it -> builder.directive(it.getName(), it.getDirective())));
        Optional.ofNullable(directiveWirings).ifPresent(wiring ->wiring.forEach(builder::directiveWiring));

        builder.resolvers(resolvers);

        return builder.build();
    }

    @Bean
    public GraphQLSchema graphQLSchema(SchemaParser schemaParser) {
        return schemaParser.makeExecutableSchema();
    }

    @Bean
    public MaxQueryDepthInstrumentation maxQueryDepthInstrumentation(){
        return new MaxQueryDepthInstrumentation(100);
    }





}
