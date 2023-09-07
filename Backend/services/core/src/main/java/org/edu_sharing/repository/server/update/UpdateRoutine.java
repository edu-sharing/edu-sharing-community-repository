package org.edu_sharing.repository.server.update;

import java.lang.annotation.*;


/**
 * Controls the execution order of Updates
 * The update will be scheduled by the given order
 * <p>
 * the update method needs to be of signature
 * public void updateMethod()
 * or
 * public void updateMethod(boolean)
 * <p>
 * The return type doesn't matter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UpdateRoutine {
    /**
     * @return A unique identifier that is stored in the database to prevent the update from being rerun after it is run
     */
    String id();

    /**
     * @return The description of the update
     */
    String description() default "";

    /**
     * @return The value indicates the order in which the update is performed in relation to other updates. If two update uses the same order number, the sequence is undetermined.
     */
    int order();


    /**
     * @return Determines whether the UpdateRoutine runs automatically at startup or should be triggered manually
     */
    boolean auto() default false;


    /**
     * @return If set the update will not be capsuled in a global transactional context
     */
    boolean isNonTransactional() default false;
}

