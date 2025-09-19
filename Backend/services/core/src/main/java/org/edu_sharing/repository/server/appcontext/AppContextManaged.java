package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;

/**
 * Marker annotation to indicate that a given interface or component is eligible for
 * app context-specific management. This annotation is primarily used in conjunction with
 * the app context mechanisms to dynamically manage and provide beans based on the
 * current application context.
 *
 * Components or interfaces annotated with {@code AppContextManaged} can be scanned
 * and managed dynamically by the app context framework, allowing for automatic
 * proxying and dependency injection that adheres to app-specific configurations.
 *
 * The annotation is typically used at the type level on interfaces that define
 * the contract for app context-aware dependency injection.
 *
 * Usage of this annotation enables the app context infrastructure to identify
 * and manage the annotated beans during runtime, thereby facilitating modular
 * and context-aware application development.
 *
 * This annotation complements {@link AppContext} and {@link LocalAppContext} as
 * part of the app context management system.
 *
 * See also:
 * - {@code AppContextRegistry}
 * - {@code AppContextServiceLocator}
 * - {@code ContextAwareProxyFactory}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AppContextManaged {
}
