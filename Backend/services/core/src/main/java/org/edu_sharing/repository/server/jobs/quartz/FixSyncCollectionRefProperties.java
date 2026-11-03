package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@JobDescription(description = "Overwrites all collection reference properties with the ones from their original nodes.")
public class FixSyncCollectionRefProperties extends AbstractJobMapAnnotationParams{

    @JobFieldDescription(description = "Custom query to use to find the references to sync. When using an own query, please make sure to filter for aspects of ccm:collection_io_reference", sampleValue = "{\"bool\":{\"must\":[{\"term\":{\"aspects\":\"ccm:collection_io_reference\"}}]}}")
    private String query;

    @JobFieldDescription(description = "Run threaded (default false)")
    private boolean threaded = false;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
        NodeService nodeService = serviceRegistry.getNodeService();

        NodeRunner nr = new NodeRunner();
        nr.setRunAsSystem(true);
        nr.setTask((ref) -> {
            if(isInterrupted()) {
                return;
            }
            try {
                // elastic index may be stale -> node may have been deleted in the meantime
                if (!nodeService.exists(ref) || !nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
                    return;
                }
                String original = (String) nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
                if (original == null || ref.getId().equals(original)) {
                    return;
                }
                NodeRef nodeRefOriginal = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, original);
                if (!nodeService.exists(nodeRefOriginal)) {
                    logger.warn("original node " + nodeRefOriginal + " of ref " + ref + " does not exists");
                    return;
                }
                logger.info("syncing: " + ref + " nodeRefOriginal:" + nodeRefOriginal);
                NodeCustomizationPolicies.syncCollectionRefProps(nodeRefOriginal, ref, nodeService.getProperties(ref), nodeService.getProperties(nodeRefOriginal), false, nodeService);
            } catch (Exception e) {
                logger.warn("error while syncing: " + ref + ": " + e.getMessage(), e);
            }
        });
        nr.setThreaded(threaded);
        // retrying: parallel transactions may run into concurrency failures, which will then be retried instead of failing the node
        nr.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
        nr.setKeepModifiedDate(true);
        nr.setTypes(Arrays.asList(new String[] { CCConstants.CCM_TYPE_IO }));
        if(StringUtils.isNotBlank(query)) {
            nr.setElastic(query);
        } else {
            nr.setElastic("{\"bool\":{\"must\":[{\"term\":{\"type\":\"ccm:io\"}},{\"term\":{\"nodeRef.storeRef.protocol\":\"workspace\"}},{\"term\":{\"aspects\":\"ccm:collection_io_reference\"}}]}}");
        }
        nr.run();

    }

}
