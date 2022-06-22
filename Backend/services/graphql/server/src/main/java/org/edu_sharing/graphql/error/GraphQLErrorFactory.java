package org.edu_sharing.graphql.error;

import graphql.GraphQLError;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

public interface GraphQLErrorFactory {

    static GraphQLErrorFactory withReflection(Object object, Method method) {
        return new ReflectiveGraphQLErrorFactory(object, method);
    }

    Optional<Class<? extends Throwable>> mostConcrete(Throwable t);

    Collection<GraphQLError> create(Throwable t, ErrorContext errorContext);
}
