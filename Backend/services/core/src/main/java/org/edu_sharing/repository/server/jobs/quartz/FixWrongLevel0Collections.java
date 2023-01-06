package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;


import java.util.Arrays;

@JobDescription(description = "checks all collections for leve0 property and sets to false when collection is a subcollection")
public class FixWrongLevel0Collections extends AbstractJob{


    @JobFieldDescription(description = "run job for log protocol but don't persist any changes")
    private boolean testMode = true;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
    ServiceRegistry serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = (NodeService)serviceRegistry.getNodeService();

    Logger logger = Logger.getLogger(FixWrongLevel0Collections.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String tm = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("testMode");
        if(tm == null) testMode = true;
        else testMode = new Boolean(tm);

        AuthenticationUtil.runAsSystem(() -> {
           execute();
           return null;
        });
    }

    private void execute(){
        NodeRef companyHome = repositoryHelper.getCompanyHome();
        NodeRef collections =  nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS,"collections");
        if(collections == null){
            logger.error("no collections folder found");
            return;
        }

        NodeRunner runner = new NodeRunner();
        runner.setTask((nodeRef)-> {
            String name = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_NAME);
            if(nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))
                    && nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
                Boolean level0 = (Boolean)nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0));
                if(Boolean.TRUE.equals(level0)){
                    ChildAssociationRef primaryParent = nodeService.getPrimaryParent(nodeRef);
                    String parentName = (String)nodeService.getProperty(primaryParent.getParentRef(),ContentModel.PROP_NAME);
                    if(nodeService.hasAspect(primaryParent.getParentRef(),QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
                        logger.info("collection "+name + "("+nodeRef+") is a subcollection of "+parentName+"("+primaryParent.getParentRef()+")");
                        if(!testMode){
                            nodeService.setProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0),false);
                        }
                    }
                }
            }
            //do something
        });
        runner.setTypes(Arrays.asList(new String[]{CCConstants.CCM_TYPE_MAP}));
        runner.setRunAsSystem(true);
        runner.setThreaded(false);
        runner.setRecurseMode(RecurseMode.Folders);
        runner.setStartFolder(collections.getId());
        runner.setKeepModifiedDate(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);

        runner.run();
    }

    @Override
    public Class[] getJobClasses() {
        this.addJobClass(FixWrongLevel0Collections.class);
        return super.allJobs;
    }
}
