package org.edu_sharing.repository.server.update;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;


/**
 * Indicates that an annotated class is a "UpdateService".
 *
 * <p>This annotation serves as a specialization of {@link Component @Component} and with prototype {@link Scope @Scope} ,
 * allowing for implementation classes to be autodetected through classpath scanning.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see Component
 * @see Repository
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Scope("prototype")
public @interface UpdateService {
}
