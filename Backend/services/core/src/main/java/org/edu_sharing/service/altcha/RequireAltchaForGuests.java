package org.edu_sharing.service.altcha;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires a valid ALTCHA payload in the {@link AltchaService#HEADER} header for guest requests
 * on the annotated JAX-RS resource method. Authenticated users are not affected.
 * Enforced by {@link AltchaGuestFilter}.
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAltchaForGuests {
    /**
     * the action the challenge must be bound to
     */
    String value() default AltchaService.ACTION_REPORT;
}
