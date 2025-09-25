package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;


/**
 * Annotation to define application context-specific components or interfaces.
 *
 * The {@code AppContext} annotation is primarily used to associate a specific value or key,
 * typically an application identifier, with a particular class. This allows the app context
 * management system to dynamically manage, resolve, and inject dependencies based on
 * specific application contexts.
 *
 * When applied to a type, the app context mechanisms can use the annotated value to identify
 * and retrieve appropriate beans or components during runtime. This enhances modularity and
 * supports multi-tenant or multi-application configurations.
 *
 * This annotation is closely related to {@code LocalAppContext} and {@code AppContextManaged},
 * which offer complementary functionality for local context definitions and app context-specific
 * component management.
 *
 * Key features:
 * - Associates a specific application identifier with a component or interface.
 * - Supports dynamic resolution of context-dependent dependencies at runtime.
 * - Facilitates application context-awareness in multi-application environments.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AppContext {
    String[] value();
}
