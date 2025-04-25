package org.edu_sharing.spring.conditions;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional({OnPropertyCondition.class})
public @interface ConditionalOnProperty {
    /**
     * Alias for {@link #name()}.
     * @return the names
     */
    String[] value() default {};

    /**
     * The name of the properties to test. If a prefix has been defined, it is applied to compute the full key of each property. For instance if the prefix is app.config and one value is my-value, the full key would be app.config.my-value
     * If multiple names are specified, all of the properties have to pass the test for the condition to match.
     * @return
     * the names
     */
    String[] name() default {};

    /**
     * A prefix that should be applied to each property. The prefix automatically ends with a dot if not specified. A valid prefix is defined by one or more words separated with dots (e.g. "acme.system.feature").
     * @return
     * the prefix
     */
    String prefix() default "";

    /**
     * The string representation of the expected value for the properties. If not specified, the property must not be equal to false.
     * @return
     * the expected value
     */
    String havingValue() default "";

    /**
     * Specify if the condition should match if the property is not set. Defaults to false.
     * @return
     * if the condition should match if the property is missing
     */
    boolean matchIfMissing() default false;
}
