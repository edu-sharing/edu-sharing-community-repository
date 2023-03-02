package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.service.usage.UsageException;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Logs all CollectionIOReference objects whose referenced nodes have no usages")
public class LogCollectionReferenceWithoutUsagesJob extends AbstractJobMapAnnotationParams {

    @Autowired
    @Setter
    private Usage2Service usage2Service;

    @Autowired
    @Setter
    private NodeService nodeService;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) {
        NodeRunner runner = new NodeRunner();
        runner.setThreaded(false);
        runner.setRunAsSystem(true);
        runner.setKeepModifiedDate(true);
        runner.setFilter((node)->
                nodeService.hasAspect(node.getStoreRef().getProtocol(),node.getStoreRef().getIdentifier(),node.getId(),CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)
        );
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
        runner.setTask(this::handleNode);
        runner.run();
    }

    public void handleNode(@NotNull NodeRef node) {
        String original = nodeService.getProperty(node.getStoreRef().getProtocol(), node.getStoreRef().getIdentifier(), node.getId(), CCConstants.CCM_PROP_IO_ORIGINAL);
        if (!nodeService.exists(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), original)) {
            logger.info("Found orphan reference " + node.getId());
            return;
        }


        try {
            List<Usage> usages = usage2Service.getUsageByParentNodeId(null, null, original);
            if(usages.isEmpty()){
                logger.info("No usages found for: " + node.getId());
            }
        } catch (UsageException e) {
            logger.error("Error while querying usages for: " + node.getId() + " " + e);
        }
    }

    @Override
    public Class[] getJobClasses() {
        return allJobs;
    }
}
