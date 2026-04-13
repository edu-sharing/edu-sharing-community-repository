package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PersonService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@JobDescription(description = "Bulk change metadata of nodes based on CSV file")
public class BulkEditNodesJobCSV extends AbstractInterruptableJob {
    @JobFieldDescription(file = true, description = "CSV containing rows of nodeId and metadata fields to be updated")
    private String csvContent;

    @JobFieldDescription(description = "Indicates whether the MDS rules must be followed when updating metadata.", sampleValue = "true")
    private boolean obeyMds = true;

    @JobFieldDescription(description = "Indicates whether the job should only perform a dry run without actually updating nodes.", sampleValue = "false")
    private boolean testRun = false;

    @JobFieldDescription(description = "Indicates whether the job should not update the modified date of the updating nodes.", sampleValue = "true")
    private boolean silentUpdate = true;

    @JobFieldDescription(description = "Mode to use", sampleValue = "NODE_ID")
    private Mode mode = Mode.NODE_ID;


    @Setter(onMethod_ = @Autowired)
    private NodeService nodeService;

    @Setter(onMethod_ = @Autowired)
    private PersonService personService;

    @Setter(onMethod_ = @Autowired)
    private RetryingTransactionHelper retryingTransactionHelper;

    @Setter(onMethod_ = @Autowired)
    private BehaviourFilter policyBehaviourFilter;


    private enum Mode {
        @JobFieldDescription(description = "Use NodeId to identify nodes (column \"nodeId\" must be used)")
        NODE_ID,
        @JobFieldDescription(description = "Use Authorities to identify nodes (column \"authority\" must be used)")
        AUTHORITIES
    }


    @Override
    protected void executeInterruptable(JobExecutionContext context) throws JobExecutionException {
        CSVTool.CSVResult csvResult = CSVTool.readCSV(new BufferedReader(new StringReader(csvContent)), ',');

        if (csvResult == null) {
            throw new JobExecutionException("Failed to parse CSV content");
        }

        if (csvResult.getHeaders().isEmpty()) {
            throw new JobExecutionException("No valid headers found in CSV content");
        }

        final String identifier = mode == Mode.NODE_ID ? "nodeId" : "authority";

        if (!csvResult.getHeaders().contains(identifier)) {
            throw new JobExecutionException(String.format("No %s column found in CSV content", identifier));
        }

        if (csvResult.getHeaders().size() < 2) {
            throw new JobExecutionException("No properties column found in CSV content");
        }

        if (csvResult.getLines().isEmpty()) {
            throw new JobExecutionException("No valid rows found in CSV content");
        }

        List<String> propertyNames = csvResult.getHeaders()
                .stream()
                .filter(x -> !x.equals(identifier))
                .collect(Collectors.toList());

        AuthenticationUtil.runAsSystem(() -> {
            int i = 0;
            for (Map<String, String> line : csvResult.getLines()) {
                Map<String, String[]> properties = propertyNames.stream().collect(Collectors.toMap(CCConstants::getValidGlobalName, x -> new String[]{line.get(x)}));
                try {
                    String nodeId;
                    if (mode == Mode.AUTHORITIES) {
                        String userName = line.get(identifier);
                        NodeRef personId = personService.getPersonOrNull(userName);
                        if (personId == null) {
                            log.warn("Authority: {} not found", userName);
                            continue;
                        }
                        nodeId = personId.getId();
                    } else {
                        nodeId = line.get(identifier);
                    }

                    if (!testRun) {
                        retryingTransactionHelper.doInTransaction(() -> {
                            if (silentUpdate) {
                                policyBehaviourFilter.disableBehaviour(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), ContentModel.ASPECT_AUDITABLE);
                            }
                            try {
                                nodeService.updateNode(nodeId, properties, obeyMds);
                            } finally {
                                if (silentUpdate) {
                                    policyBehaviourFilter.enableBehaviour(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), ContentModel.ASPECT_AUDITABLE);
                                }
                            }
                            return null;
                        });
                    }

                    log.info("Node with ID: {} updated values: {}", line.get(identifier), properties.entrySet().stream().map(entry -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()[0])).collect(Collectors.joining(", ")));
                    i++;
                } catch (Throwable e) {
                    log.warn("Failed to update node with ID: {} due to: {}", line.get(identifier), e.getMessage());
                }
            }
            log.info("Total nodes updated: {} of {}", i, csvResult.getLines().size());
            return null;
        });


    }

}
