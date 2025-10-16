package org.edu_sharing.repository.server.jobs;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.jobs.ibatis.JobQueueMapper;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
public class JobQueueContext {
    /**
     * Indicates whether queuing for jobs in the job queue context is disabled.
     * When set to <code>true</code>, Queue annotated methods will be executed immediately,
     * When set to <code>false</code>, jobs will be queued as normal.
     *
     * This flag can be useful in scenarios where queuing needs to be conditionally
     * enabled or disabled based on specific application requirements or runtime
     * conditions.
     */
    @Getter
    @Setter
    private boolean disableQueuing = false;

    @Setter(AccessLevel.PACKAGE)
    private JobQueueEntry queuedJob;

    @Setter(AccessLevel.PACKAGE)
    private JobQueueMapper jobQueueMapper;

    private final List<JobQueueEntry> queuedJobs = new java.util.ArrayList<>();

    void addQueuedJob(JobQueueEntry jobQueueEntry) {
        queuedJobs.add(jobQueueEntry);
    }

    public List<JobQueueEntry> getQueuedJobs() {
        return Collections.unmodifiableList(queuedJobs);
    }
    
    /**
     * Extends the time-to-live (TTL) of the current queue job by updating its last updated timestamp.
     * If the current queued job or the job queue mapper is not initialized, the method will exit
     * without making any updates and log a debug message.
     *
     * The method sets the current system date and time as the last updated timestamp in the
     * current queued job and then updates this information in the database via the job queue mapper.
     *
     * This method ensures that the current queue job's TTL is effectively extended, allowing the
     * job to remain valid for processing based on its updated timestamp.
     */
    public void extendJobTtl() {
        if (queuedJob == null || jobQueueMapper == null) {
            log.debug("no jobqueue entry or jobqueue mapper");
            return;
        }
        log.debug("extending ttl of jobqueue entry {}", queuedJob);
        queuedJob.setLastUpdated(new Date());
        jobQueueMapper.updateLastUpdated(queuedJob);
    }

    /**
     * Checks whether the TTL (time-to-live) of the current queued job has expired.
     * The TTL expiration is determined by adding the TTL duration to the last updated
     * timestamp of the current queued job and comparing it to the current system time.
     * Returns false if the TTL is not defined, negative, zero, or if any required data
     * such as the last updated timestamp is missing.
     *
     * @return true if the TTL of the current queued job has expired, false otherwise
     *         or in cases where required data is not available or invalid.
     */
    public boolean isTtlExpired() {
        if (queuedJob == null) {
            log.debug("no jobqueue entry");
            return false;
        }

        Date lastUpdated = queuedJob.getLastUpdated();
        if (lastUpdated == null) {
            log.debug("no last updated date");
            return false;
        }

        Duration ttl = queuedJob.getTtl();
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            log.debug("no ttl or ttl is negative or zero");
            return false;
        }


        Instant last = lastUpdated.toInstant();
        Instant expiry;
        try {
            expiry = last.plus(ttl);
        } catch (ArithmeticException | DateTimeException ex) {
            log.error(ex.getMessage(), ex);
            return true;
        }

        Instant now = Instant.now();
        return expiry.isBefore(now);
    }
}
