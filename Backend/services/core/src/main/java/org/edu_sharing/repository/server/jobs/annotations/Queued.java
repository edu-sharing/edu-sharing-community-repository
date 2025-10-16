package org.edu_sharing.repository.server.jobs.annotations;

import org.edu_sharing.repository.server.jobs.JobQueueContext;
import org.edu_sharing.repository.server.jobs.JobQueueContextHolder;

import java.lang.annotation.*;

/**
 * Annotation that marks a method to be queued as a job instead of being executed immediately.
 * The method will be invoked by the job scheduler from any instance in the cluster.
 * 
 * The annotated method can have any type of arguments, which can be serialized as JSON (no cyclic dependencies allowed and no unserializable types). 
 * It will typically have a void return type; if not, the returned value will be ignored when called through the scheduler.
 * At the moment of queuing the job, the AOP Proxy will return a default value based on the return type instead.
 *
 * HINT: see {@link JobQueueContextHolder#getJobQueueContext()} to get the current {@link JobQueueContext}.
 *
 * @see JobQueueContextHolder
 * @see JobQueueContext
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Queued {
    /**
     * Specifies the group to which the queued job belongs to.
     * Jobs within the same group will be exclusively executed by a single worker thread.
     * So that jobs within the same group are processed in order one after another.
     *
     * If not set jobs will be executed in parallel.
     *
     * @return the name of the group associated with the job, or an empty string if no group is specified
     */
    String group() default "";
    /**
     * Indicates whether the queued job should be unique. If set to true,
     * the system ensures that there are no other active or pending jobs
     * with the same characteristics to avoid duplication.
     *
     * Characteristics of a job are defined by its annotated bean, method signature, provided parameter values and authenticated user who scheduled the job.
     *
     * @return true if the job must be unique by its characteristics; false otherwise
     */
    boolean unique() default false;

    /**
     * Specifies the time-to-live (TTL) duration for the queued job. The TTL defines
     * how long the queued job remains active before it is considered expired.
     *
     * The duration String can be in several formats:
     * a plain integer — which is interpreted to represent a duration in milliseconds
     * any of the known DurationFormat.Style: the ISO8601 style or the SIMPLE style
     *
     * @return a string representing the TTL duration of the job — for example a placeholder, or a java.time.Duration compliant value or a simple format compliant value
     */
    String ttl() default "12h";
}
