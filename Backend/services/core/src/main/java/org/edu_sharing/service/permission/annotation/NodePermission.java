package org.edu_sharing.service.permission.annotation;

import java.lang.annotation.*;

/**
 * Checks user permissions for a specific node
 * Requires @Permission annotation on the method
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodePermission {
    String[] value();
}
