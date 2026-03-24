package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
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
public class TouchNodesJob extends FixElasticSearchBase{

    @JobFieldDescription(description = "either to keep modified date or not", sampleValue = "true")
    boolean keepModifiedDate = true;

    @JobFieldDescription(description = "if false (default) no changes will be done.", sampleValue = "false")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched.", sampleValue = "{\"term\":{\"path\":\"<parent-uuid>\"}}")
    protected String query;

    @Autowired
    private NodeService nodeService;
    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AuthenticationUtil.runAsSystem(() -> {
            try {
                Query.Builder builder = getBuilder(query);
                search( builder.build(), new TouchHandler());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        });
    }

    public class TouchHandler implements SearchResultHandler {

        @Override
        public void handleSearchHit(Hit<Map> searchHit) {
            NodeRef nodeRef = getNodeRef(searchHit);
            String name = nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), CCConstants.CM_NAME);
            if(name == null){
                logger.warn("ignoring node cause it has no name:" +nodeRef);
                return;
            }

            logger.info("touching node:"+nodeRef +" name:"+name);
            if(execute){
                retryingTransactionHelper.doInTransaction(() -> {
                    nodeService.touch(nodeRef.getId(), keepModifiedDate);
                    return null;
                },false,true);
            }
        }
    }
}
