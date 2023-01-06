package org.edu_sharing.service.nodeservice.annotation;

import java.lang.annotation.*;

/**
 * Maps the incoming node id automatically on the original node id
 * i.e. collection refs to original ids
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeOriginal {
}
