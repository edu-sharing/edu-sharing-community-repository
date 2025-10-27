package org.edu_sharing.repository.server.tools.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryingTransaction {
    boolean readonly() default false;
    boolean requiresNew() default false;
}
