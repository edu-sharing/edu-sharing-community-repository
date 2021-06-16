package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class SyncOrganisationFolderName extends AbstractJob{

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService = (org.edu_sharing.alfresco.service.OrganisationService) applicationContext
            .getBean("eduOrganisationService");

    public static String PARAM_EXECUTE = "EXECUTE";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AuthenticationUtil.runAsSystem(() -> {
            eduOrganisationService.syncOrganisationFolderName(new Boolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE)));
            return null;
        });

    }

    @Override
    public Class[] getJobClasses() {
        this.addJobClass(SyncOrganisationFolderName.class);
        return this.allJobs;
    }
}
