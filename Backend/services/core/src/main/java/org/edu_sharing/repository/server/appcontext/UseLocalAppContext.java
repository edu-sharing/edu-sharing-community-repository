package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;

/**
 * Annotation to indicate the use of the local application context for a specific field or parameter.
 *
 * The {@code UseLocalAppContext} annotation is used in the app context framework to signify that
 * the annotated field or parameter should use dependencies, services, or beans from the local
 * application context rather than a global or shared context. This is useful in scenarios where
 * the local application requires specific configurations or services distinct from those of other
 * applications or tenants in a multi-application environment.
 *
 * Key Features:
 * - Marks a field or parameter to use dependencies specific to the local application context.
 * - Enables context-aware dependency injection localized to the individual application.
 * - Facilitates modular and isolated configurations for multi-application systems.
 *
 * This annotation works in conjunction with other app context-related annotations like {@code AppContext},
 * {@code UseAppContext}, and {@code LocalAppContext} to provide a comprehensive framework for
 * managing dependencies in complex application architectures.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseLocalAppContext {
}
