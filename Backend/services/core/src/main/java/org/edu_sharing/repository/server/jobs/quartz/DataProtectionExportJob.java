package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.service.dataprotection.DataProtectionQueue;
import org.edu_sharing.service.dataprotection.DataProtectionService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class DataProtectionExportJob extends AbstractJobMapAnnotationParams{

    @Autowired
    DataProtectionService dataProtectionService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        dataProtectionService.startExport();
    }


}
