package org.edu_sharing.graphql.tools;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.kickstart.tools.SchemaParser;
import graphql.kickstart.tools.SchemaParserBuilder;
import graphql.schema.GraphQLSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
public class GraphQLJavaToolsConfiguation {

    @Bean
    public SchemaStringProvider schemaStringProvider(ApplicationContext applicationContext){
        return new ClasspathResourceSchemaStringProvider("**/*.graphqls");
    }

    @Bean
    public SchemaParser schemaParser(
            List<GraphQLResolver<?>> resolvers,
            SchemaStringProvider schemaStringProvider) throws IOException {
        SchemaParserBuilder builder = new SchemaParserBuilder();

        List<String> schemaStrings = schemaStringProvider.schemaStrings();
        schemaStrings.forEach(builder::schemaString);
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
