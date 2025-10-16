package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.service.dataprotection.DataProtectionService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@JobDescription(description = "processes dataprotection requests")
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class DataProtectionExportJob extends AbstractInterruptableJob{

    @Autowired
    DataProtectionService dataProtectionService;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) {
        log.info("DataProtectionExportJob start");
        dataProtectionService.cleanExpired();
        log.info("DataProtectionExportJob end");
    }
}
