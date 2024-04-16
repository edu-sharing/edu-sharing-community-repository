package org.edu_sharing.service.nodeservice.annotation;

import java.lang.annotation.*;

/**
 * Generic method annotation for node manipulation parameter annotation
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeManipulation {
}
