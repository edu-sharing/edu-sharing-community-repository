package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.content.ContentStore;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Date;

/**
 * Cleans up the alfresco deleted content store (contentstore.deleted).
 * <p>
 * When alfresco purges nodes (e.g. emptying the trashcan / orphan cleanup) their binary content is
 * moved into the deleted content store instead of being removed immediately. Those files stay there
 * forever unless they are purged. This job removes files that have not been modified for at least
 * {@link #minPurgeAgeDays} days and cleans up the empty directories left behind.
 * <p>
 * The job is inactive by default - it is not registered in edu-sharing.reference.conf. To enable it,
 * add an entry to {@code jobs.entries} (e.g. with a Cron trigger) and override the parameters as needed:
 * <pre>
 * jobs.entries += {
 *     name: "Deleted Content Store Cleanup"
 *     class: "org.edu_sharing.repository.server.jobs.quartz.DeletedContentStoreCleanupJob"
 *     trigger: "Cron[0 0 3 * * ?]"   // nightly at 03:00
 *     params: { minPurgeAgeDays: 30, execute: true }
 * }
 * </pre>
 * For safety {@link #execute} defaults to {@code false} (dry run), so a first run only logs what would
 * be purged.
 * <p>
 * Inspired by https://github.com/keensoft/alfresco-deleted-content-store-cleaner
 */
@JobDescription(description = "Cleans up the alfresco deleted content store (contentstore.deleted) by purging files older than 'minPurgeAgeDays' and removing the empty folders left behind. Inactive by default; enable via a jobs.entries entry. execute=false (default) performs a dry run that only logs what would be removed.")
public class DeletedContentStoreCleanupJob extends AbstractJobMapAnnotationParams {

    Logger logger = Logger.getLogger(DeletedContentStoreCleanupJob.class);

    @JobFieldDescription(description = "minimum age in days a file in the deleted content store must reach (based on last modification) before it is purged.", sampleValue = "30")
    int minPurgeAgeDays = 30;

    @JobFieldDescription(description = "if false (default) nothing is deleted; files that would be purged are only logged (dry run).")
    boolean execute = false;

    private int deletedFiles;
    private int deletedFolders;
    private long freedBytes;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ContentStore deletedContentStore = (ContentStore) applicationContext.getBean("deletedContentStore");
        String rootLocation = deletedContentStore.getRootLocation();

        File rootFolder = new File(rootLocation);
        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            logger.warn("deleted content store root location does not exist or is not a directory: "
                    + rootFolder.getAbsolutePath() + ". nothing to do.");
            return;
        }

        long maxModifiedTime = System.currentTimeMillis() - ((long) minPurgeAgeDays) * 24L * 3600L * 1000L;

        logger.info("DeletedContentStoreCleanupJob starts. " + (execute ? "" : "DRY RUN (execute=false) - ")
                + "about to remove deleted files modified before " + new Date(maxModifiedTime)
                + " from deleted content store at " + rootFolder.getAbsolutePath()
                + " (minPurgeAgeDays=" + minPurgeAgeDays + ")");

        File[] files = rootFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isInterrupted()) {
                    logger.info("DeletedContentStoreCleanupJob was interrupted, aborting.");
                    break;
                }
                purgeAbandonedFiles(file, maxModifiedTime);
            }
        }

        logger.info("DeletedContentStoreCleanupJob ends. " + (execute ? "removed " : "would remove ")
                + deletedFiles + " file(s) and " + deletedFolders + " empty folder(s), freeing "
                + freedBytes + " byte(s).");
    }

    private void purgeAbandonedFiles(File file, long maxModifiedTime) {
        if (isInterrupted()) {
            return;
        }
        if (file.isFile()) {
            if (file.lastModified() < maxModifiedTime) {
                long size = file.length();
                if (execute) {
                    if (file.delete()) {
                        deletedFiles++;
                        freedBytes += size;
                        logger.debug("removed file " + file.getAbsolutePath());
                    } else {
                        logger.warn("could not remove file " + file.getAbsolutePath());
                    }
                } else {
                    deletedFiles++;
                    freedBytes += size;
                    logger.debug("would remove file " + file.getAbsolutePath());
                }
            }
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (isInterrupted()) {
                        return;
                    }
                    purgeAbandonedFiles(child, maxModifiedTime);
                }
            }
            // remove the folder if it is empty now (i.e. all of its content has been purged)
            File[] remaining = file.listFiles();
            if (remaining != null && remaining.length == 0) {
                if (execute) {
                    if (file.delete()) {
                        deletedFolders++;
                        logger.debug("removed empty folder " + file.getAbsolutePath());
                    } else {
                        logger.warn("could not remove empty folder " + file.getAbsolutePath());
                    }
                } else {
                    deletedFolders++;
                    logger.debug("would remove empty folder " + file.getAbsolutePath());
                }
            }
        }
    }
}
