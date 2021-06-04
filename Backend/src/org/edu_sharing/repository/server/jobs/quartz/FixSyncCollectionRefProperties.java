package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@JobDescription(description = "Overwrites all collectionreference properties with the original ones.")
public class FixSyncCollectionRefProperties extends AbstractJob{

    Logger logger = Logger.getLogger(FixSyncCollectionRefProperties.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
        NodeService nodeService = serviceRegistry.getNodeService();

        NodeRunner nr = new NodeRunner();
        nr.setRunAsSystem(true);
        nr.setTask((ref) -> {
            if(nodeService.getAspects(ref).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
                String original = (String)nodeService.getProperty(ref,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
                if(original != null && !ref.getId().equals(original)){
                    NodeRef nodeRefOriginal = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,original);
                    try {
                        logger.info("syncing: "+ref+" nodeRefOriginal:"+nodeRefOriginal);
                        NodeCustomizationPolicies.syncCollectionRefProps(nodeRefOriginal,ref,null, nodeService.getProperties(nodeRefOriginal),false, nodeService);
                    } catch (Exception e) {
                        logger.error(e.getMessage(),e);
                    }
                }

            }
        });
        nr.setTransaction(NodeRunner.TransactionMode.Local);
        nr.setKeepModifiedDate(true);
        nr.setTypes(Arrays.asList(new String[] { CCConstants.CCM_TYPE_IO }));
        nr.setLucene("TYPE:\"ccm:io\" AND ASPECT:\"ccm:collection_io_reference\"");
        nr.run();

    }

}
