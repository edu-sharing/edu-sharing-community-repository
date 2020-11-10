package org.edu_sharing.repository.server.jobs.quartz.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JobFieldDescription {
    String description() default "";
    String sampleValue() default "";
    boolean file() default false;
}
