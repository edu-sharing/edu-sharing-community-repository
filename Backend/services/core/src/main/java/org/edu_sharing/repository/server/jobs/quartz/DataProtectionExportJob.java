package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.service.dataprotection.DataProtectionService;
import org.edu_sharing.service.dataprotection.DataProtectionQueue;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DataProtectionExportJob extends AbstractJobMapAnnotationParams{

    @Autowired
    DataProtectionService DataProtectionService;

    @Autowired
    DataProtectionQueue taskService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
       List<String> allUsers = taskService.getAllUsers();
       for(String user: allUsers) {
           DataProtectionService.startExport(user);
           taskService.removeUsers(List.of(user));
       }
    }


}
