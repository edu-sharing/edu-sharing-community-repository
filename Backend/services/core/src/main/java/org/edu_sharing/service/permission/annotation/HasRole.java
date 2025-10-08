package org.edu_sharing.service.permission.annotation;

import java.lang.annotation.*;

/**
 * Checks user has the specified authority
 * Requires @Permission annotation on the method
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasRole {
}
