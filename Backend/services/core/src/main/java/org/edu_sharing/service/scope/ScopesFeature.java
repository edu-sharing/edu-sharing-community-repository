package org.edu_sharing.service.scope;

import java.lang.annotation.*;

/**
 * Binds a bean (type level) or a method to the feature flag repository.scopes.enabled.
 * While the flag is off, calls throw {@link ScopesDisabledException} (REST: 404), see {@link ScopesFeatureAspect}.
 * <p>
 * Method level annotations win over the type level and are also found on the interface method, so
 * {@code requiresEnabled = false} on an interface method exempts it in every implementation
 * (e.g. the change feed for the tracker or the purge hooks, which also have to work while the flag is off).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ScopesFeature {
    boolean requiresEnabled() default true;
}
