package org.edu_sharing.repository.server.jobs.quartz;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.lifecycle.OrganisationLifecycleService;
import org.edu_sharing.service.lifecycle.OrganisationLifecycleServiceTestSetup;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Arrays;
import java.util.List;

@JobDescription(description = "deletes organisation with all members (users and subgroups) and files,folders,collections")
public class OrganisationLifecycleJob extends AbstractInterruptableJob{

    @JobFieldDescription(description = "list of organisations to cleanup. is required.")
    List<String> organisations;

    @JobFieldDescription(description = "if true an test org with users and file/folder structure will be created")
    boolean testSetup = false;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if(testSetup){
            new OrganisationLifecycleServiceTestSetup();
        }else{
            AuthenticationUtil.runAsSystem(() -> {
                for(String organisation : organisations){
                    new OrganisationLifecycleService(organisation).deleteOrganisation();
                }
                return null;
            });
        }
    }
}
