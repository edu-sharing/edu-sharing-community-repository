package org.edu_sharing.service.permission.annotation;

import java.lang.annotation.*;

/**
 * Enables user permission check for the annotated method
 * throws InsufficientPermissionException if permission is not set
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    String[] value() default {};
    boolean requiresUser() default false;
}
