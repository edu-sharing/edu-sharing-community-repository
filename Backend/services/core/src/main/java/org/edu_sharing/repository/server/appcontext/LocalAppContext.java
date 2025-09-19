package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;


/**
 * Marker annotation to designate a class or component as applicable to the local application context.
 *
 * The {@code LocalAppContext} annotation is used in the app context infrastructure to identify
 * specific components or interfaces that are context-aware specifically for the local application.
 * This allows for the isolation and management of dependencies and beans that are unique to
 * the application's local environment.
 *
 * Components annotated with {@code LocalAppContext} can be automatically resolved during dependency injection
 * when the app context mechanism encounters the need for a local-only implementation or configuration.
 * It is commonly used in scenarios where certain services or beans should be differentiated
 * between applications within a multi-application system.
 *
 * Key Features:
 * - Enables explicit tagging of classes or components for local application usage.
 * - Aids the dynamic resolution of beans specific to the local app context.
 * - Works as part of the broader app context infrastructure alongside {@code AppContext} and {@code AppContextManaged}.
 *
 * Applicable context resolution order:
 * 1. Application-specific components resolved via the {@link AppContext} annotation.
 * 2. Fallback to local-specific components designated by {@code LocalAppContext}.
 *
 * Usage of this annotation simplifies the app context framework's ability to determine the appropriate
 * local dependencies at runtime while maintaining modular and flexible configuration patterns.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LocalAppContext {
}
