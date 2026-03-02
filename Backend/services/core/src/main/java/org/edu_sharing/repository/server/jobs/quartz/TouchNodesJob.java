package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.transaction.*;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.Map;

@JobDescription(description = "touch nodes so they get re-indexed by search index, in case they have a wrong state in the index")
public class TouchNodesJob extends FixElasticSearchBase {

    @JobFieldDescription(description = "either to keep modified date or not", sampleValue = "true")
    boolean keepModifiedDate = true;

    @JobFieldDescription(description = "if false (default) no changes will be done.", sampleValue = "false")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched.", sampleValue = "{\"term\":{\"path\":\"<parent-uuid>\"}}")
    protected String query;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AuthenticationUtil.runAsSystem(() -> {
            try {
                Query.Builder builder = getBuilder(query);
                search(builder.build(), new TouchHandler());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        });
    }

    public class TouchHandler implements SearchResultHandler {

        @Override
        public void handleSearchHit(Hit<Map> searchHit) throws IOException {
            try {
                NodeRef nodeRef = getNodeRef(searchHit);
                String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
                if (name == null) {
                    logger.warn("ignoring node cause it has no name:" + nodeRef);
                    return;
                }

                logger.info("touching node:" + nodeRef + " name:" + name);
                if (execute) {
                    RetryingTransactionHelper th = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
                    th.doInTransaction(() -> {
                        try {
                            if (keepModifiedDate) policyBehaviourFilter.disableBehaviour(nodeRef);
                            nodeService.addAspect(nodeRef, ContentModel.ASPECT_INDEX_CONTROL, null);
                        } finally {
                            nodeService.removeAspect(nodeRef, ContentModel.ASPECT_INDEX_CONTROL);
                            if (keepModifiedDate) policyBehaviourFilter.enableBehaviour(nodeRef);
                        }
                        return null;
                    }, false, true);
                }
            } catch (Throwable t) {
                logger.warn("Error while trying to touch node " + getNodeRef(searchHit) + ": " + t.getMessage(), t);
            }
        }
    }
}
