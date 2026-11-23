package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.statusreport.StatusReportService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sends the status report (versions of repository, plugins and connected systems) to the central server configured
 * in {@code repository.statusReport}. Scheduled by default via {@code jobs.defaultEntries.statusReport}, the interval
 * is its cron trigger. Like all quartz jobs it only runs on the primary repository.
 */
@JobDescription(description = "Sends the status report (versions of repository, plugins and connected systems) to the central server configured in repository.statusReport. Does nothing while it is not configured. dryRun=true only logs the report.")
public class StatusReportJob extends AbstractJobMapAnnotationParams {

    @Autowired
    private StatusReportService statusReportService;

    @JobFieldDescription(description = "if true, the report is only written to the job log and not sent")
    boolean dryRun = false;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            statusReportService.run(dryRun);
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }
}
