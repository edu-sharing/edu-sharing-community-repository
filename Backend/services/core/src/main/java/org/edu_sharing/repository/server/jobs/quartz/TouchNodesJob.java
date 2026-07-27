package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "touch nodes so they get re-indexed by search index, in case they have a wrong state in the index")
public class TouchNodesJob extends FixElasticSearchBase {

    @JobFieldDescription(description = "either to keep modified date or not", sampleValue = "true")
    boolean keepModifiedDate = true;

    @JobFieldDescription(description = "if false (default) no changes will be done.", sampleValue = "false")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched. Mutually exclusive with 'startFolder'.", sampleValue = "{\"term\":{\"path\":\"<parent-uuid>\"}}")
    protected String query;

    @JobFieldDescription(description = "folder id to start from. When set, the nodes are collected recursively via the node runner instead of the search index. Mutually exclusive with 'query'.")
    private String startFolder;

    @Autowired
    private NodeService nodeService;
    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (StringUtils.isNotBlank(query) && StringUtils.isNotBlank(startFolder)) {
            throw new IllegalArgumentException("Only one of parameters 'query' and 'startFolder' may be set");
        }
        AuthenticationUtil.runAsSystem(() -> {
            if (StringUtils.isNotBlank(startFolder)) {
                NodeRunner runner = new NodeRunner();
                runner.setTask(this::touch);
                runner.setRunAsSystem(true);
                runner.setThreaded(false);
                runner.setStartFolder(startFolder);
                runner.setKeepModifiedDate(keepModifiedDate);
                runner.setTransaction(NodeRunner.TransactionMode.Local);
                int count = runner.run();
                logger.info("Processed " + count + " nodes");
            } else {
                try {
                    Query.Builder builder = getBuilder(query);
                    search(builder.build(), new TouchHandler());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return null;
        });
    }

    private void touch(NodeRef nodeRef) {
        String name = nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), CCConstants.CM_NAME);
        if (name == null) {
            logger.warn("ignoring node cause it has no name:" + nodeRef);
            return;
        }

        logger.info("touching node:" + nodeRef + " name:" + name);
        if (execute) {
            retryingTransactionHelper.doInTransaction(() -> {
                nodeService.touch(nodeRef.getId(), keepModifiedDate);
                return null;
            }, false, true);
        }
    }

    public class TouchHandler implements SearchResultHandler {

        @Override
        public void handleSearchHit(Hit<Map> searchHit) {
            touch(getNodeRef(searchHit));
        }
    }
}
