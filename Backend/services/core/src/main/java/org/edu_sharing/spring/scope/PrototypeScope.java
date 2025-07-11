package org.edu_sharing.spring.scope;

import org.springframework.context.annotation.Scope;

import java.lang.annotation.*;

@Documented
@Scope("prototype")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PrototypeScope {
}
