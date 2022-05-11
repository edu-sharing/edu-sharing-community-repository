package org.edu_sharing.lightbend;

import java.lang.annotation.*;

/**
 * Annotation for externalized configuration. Add this to a
 * class definition if you wamt tp bind and validate some external Properties (from a hocon .conf file)
 *
 * Binding is performed by calling setters on the annotated class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ConfigurationProperties {

    /**
     * The prefix of the properties that are valid to bind to this object. A valid prefix is
     * definded by one or more words separated with dots (e.g. "repository.system.feature").
     * @Returns: the prefix of the properties to bind
     */
    String prefix();
}
