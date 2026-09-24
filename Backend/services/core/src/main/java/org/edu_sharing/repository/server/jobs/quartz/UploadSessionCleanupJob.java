package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.upload.ChunkedUploadService;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Removes chunked node content uploads (see {@code NodeApi}'s {@code /content/uploads} endpoints)
 * that were never completed or aborted, e.g. because the browser tab was closed mid-upload, and
 * fails sessions whose background processing (virus scan, metadata extraction, ...) got stuck
 * because the pod handling them was killed.
 * <p>
 * Registered by default in edu-sharing.reference.conf ({@code jobs.entries}), running every 30
 * minutes with a 24h TTL. Override via {@code jobs.entries} to change the schedule or TTL:
 * <pre>
 * jobs.entries += {
 *     name: "Upload Session Cleanup Job"
 *     class: "org.edu_sharing.repository.server.jobs.quartz.UploadSessionCleanupJob"
 *     trigger: "Cron[0 0/30 * * * ?]"   // every 30 minutes
 *     params: { ttlHours: 24 }
 * }
 * </pre>
 * Safe to run concurrently on every repository pod, since it only ever purges sessions whose
 * inactivity already exceeds the configured TTL.
 */
@JobDescription(description = "Purges chunked node content upload sessions that have been inactive for longer than 'ttlHours' " +
        "and fails sessions stuck in PROCESSING. Registered by default via jobs.entries.")
public class UploadSessionCleanupJob extends AbstractJobMapAnnotationParams {

    Logger logger = Logger.getLogger(UploadSessionCleanupJob.class);

    @JobFieldDescription(description = "upload sessions inactive for longer than this (in hours) are purged.", sampleValue = "24")
    int ttlHours = 24;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();
        ChunkedUploadService chunkedUploadService = applicationContext.getBean(ChunkedUploadService.class);

        long ttlMillis = TimeUnit.HOURS.toMillis(ttlHours);
        logger.info("UploadSessionCleanupJob starts, purging upload sessions inactive for more than " + ttlHours + " hour(s)");
        chunkedUploadService.cleanup(ttlMillis);
        logger.info("UploadSessionCleanupJob ends");
    }
}
