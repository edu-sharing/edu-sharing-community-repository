package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Date;

@JobDescription(description = "checks if license is valid using ccm:license_to. switches inherit permissions on/off")
public class LicenseManagerJob extends AbstractJob{

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    PermissionService permissionService = serviceRegistry.getPermissionService();

    Logger logger = Logger.getLogger(LicenseManagerJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        run();
    }

    public void run(){
        NodeRunner runner = new NodeRunner();
        runner.setTask((ref) -> {
            Date date = (Date)nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LICENSE_TO));
            if(date.getTime() < new Date().getTime()){
                logger.info("remove inherit parent permission for: "+ref);
                permissionService.setInheritParentPermissions(ref,false);
                nodeService.setProperty(ref,QName.createQName(CCConstants.CCM_PROP_RESTRICTED_ACCESS),true);
            }else{
                logger.info("add inherit parent permission for: "+ref);
                permissionService.setInheritParentPermissions(ref,true);
                nodeService.setProperty(ref,QName.createQName(CCConstants.CCM_PROP_RESTRICTED_ACCESS),false);
            }
        });
        runner.setTypes(Arrays.asList(new String[]{CCConstants.CCM_TYPE_IO}));
        runner.setRunAsSystem(true);
        runner.setThreaded(false);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setKeepModifiedDate(true);
        runner.setLucene("ISNOTNULL:\"ccm:license_to\"");
        int count=runner.run();
    }
}
