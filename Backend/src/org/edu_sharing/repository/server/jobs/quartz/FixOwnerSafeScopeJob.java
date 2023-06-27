package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceImpl;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;


public class FixOwnerSafeScopeJob extends AbstractJob{

    Logger logger = Logger.getLogger(FixOwnerSafeScopeJob.class);
    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
    //using alfrescoDefaultDbNodeService to run through shared folders only once (not every userhome)
    NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");


    OrganisationService eduOrganisationService = (OrganisationService)applicationContext.getBean("eduOrganisationService");

    OwnableService ownableService = serviceRegistry.getOwnableService();

    @JobFieldDescription(description = "run job in persist mode false/true", sampleValue = "false")
    boolean persist = false;

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String persistParam = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("persist");
        persist = new Boolean(persistParam);

        /**
         * run for safe userhomes
         */
        logger.info("starting for safe userhomes. persist:"+persist);
        NodeRef root = AuthenticationUtil.runAsSystem(() -> {
            NodeRef tmp = new ScopeUserHomeServiceImpl().getRootNodeRef(CCConstants.CCM_VALUE_SCOPE_SAFE);
            return tmp;
        });

        NodeRunner nr = getBasicNodeRunner();
        nr.setTask((ref) -> {
           checkAndFixOwner(ref);
        });
        nr.setStartFolder(root.getId());
        nr.run();

        /**
         * run for safe shared folders
         */
        logger.info("starting for safe shared folders. persist:"+persist);
        nr = getBasicNodeRunner();
        NodeRef orgFolderRoot = AuthenticationUtil.runAsSystem(() -> {
            return eduOrganisationService.getOrganisationFolderRoot();
        });
        nr.setTask((ref) -> {
            if(CCConstants.CCM_VALUE_SCOPE_SAFE
                    .equals(nodeService.getProperty(ref,QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME)))){
                checkAndFixOwner(ref);
            }
        });
        nr.setStartFolder(orgFolderRoot.getId());
        nr.run();

    }

    private NodeRunner getBasicNodeRunner(){
        NodeRunner nr = new NodeRunner();
        nr.setRunAsSystem(true);
        nr.setTransaction(NodeRunner.TransactionMode.Local);
        nr.setKeepModifiedDate(true);
        nr.setTypes(Arrays.asList(new String[] { CCConstants.CCM_TYPE_IO, CCConstants.CCM_TYPE_MAP }));
        return nr;
    }

    void checkAndFixOwner(NodeRef ref){
        String path = nodeService.getPath(ref).toDisplayPath(nodeService,serviceRegistry.getPermissionService());


        String pathIncludingNode = path + "/" +nodeService.getProperty(ref, ContentModel.PROP_NAME);
        logger.info(pathIncludingNode);
        if(!ownableService.hasOwner(ref)){
            String creator = (String)nodeService.getProperty(ref, ContentModel.PROP_CREATOR);
            if(creator != null && !creator.trim().isEmpty()){
                logger.info("adding owner for "+pathIncludingNode +" "+ref +" owner:" + creator);
                if(persist){
                    ownableService.setOwner(ref,creator);
                }
            }else{
                logger.info("no creator found can not determine owner for "+pathIncludingNode +" "+ref);
            }

        }
    }
}
