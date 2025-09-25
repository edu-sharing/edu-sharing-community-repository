package org.edu_sharing.repository.server.appcontext;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FallbackAppContext {
    String[] value();
}
