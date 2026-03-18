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
     * be careful using isNonTransactional=false when you're expecting the routine handle a lot of data.
     * there are ibatis cache's that fill up without limit within an db session
     * look at HierarchicalXMLConfigBuilder
     *    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
     *    it seems there is an IBatis Session for every transaction
     */
    boolean isNonTransactional() default false;

    /**
     * Specifies whether the update routine should run asynchronously.
     *
     * @return true if the update routine is executed asynchronously, false if executed synchronously.
     */
    boolean async() default false;
    
    /**
     * Specifies whether the update service should wait for the update routine to finish.
     * This setting is only relevant if {@code async} is {@code true}.
     *
     * @return {@code true} if the update service should wait for the update routine to complete;
     * {@code false} otherwise
     */
    boolean blocking() default true;
}

