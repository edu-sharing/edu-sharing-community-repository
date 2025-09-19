package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;

/**
 * An annotation to specify the use of a specific application context value
 * for fields or parameters in the app context management system.
 *
 * The {@code UseAppContext} annotation is typically applied to fields or
 * method parameters to dynamically inject or resolve components, services,
 * or dependencies tied to a particular app context value. This enables
 * fine-grained control and modularity within multi-application or
 * multi-tenant environments.
 *
 * Key Features:
 * - Associates a specific app context value with the annotated field or parameter.
 * - Facilitates dependency injection and context-aware service resolution.
 * - Supports modular design for multi-application configurations.
 *
 * The value specified in the annotation indicates the key or identifier
 * of the application context to be used for resolving the dependency.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseAppContext {
    String value();
}
