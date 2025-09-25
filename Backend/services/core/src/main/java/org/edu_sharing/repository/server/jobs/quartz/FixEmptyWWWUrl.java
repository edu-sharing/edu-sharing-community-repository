package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "removes ccm:wwwurl property from nodes when value is an empty string")
public class FixEmptyWWWUrl extends AbstractJobMapAnnotationParams {


    @Autowired
    private NodeService nodeService;

    @Autowired
    private org.edu_sharing.service.search.SearchService searchService;

    @Autowired
    private BehaviourFilter policyBehaviourFilter;

    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @JobFieldDescription(description = "if false job just logs which nodes are found and would be updated")
    boolean persistentMode = false;

    int counter = 0;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AuthenticationUtil.RunAsWork<Void> runAs = () -> {
            process();
            return null;
        };
        AuthenticationUtil.runAsSystem(runAs);
        log.info("counter: {}", counter);
    }


    private void process() {

        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(Integer.MAX_VALUE);
        searchToken.setElasticQuery(QueryBuilders.bool()
                .must(m -> m.exists(e -> e.field("properties.ccm:wwwurl")))
                .must(m -> m.term(t -> t.field("properties.ccm:wwwurl.keyword").value("")))
                .build());

        SearchResultNodeRef search = searchService.search(searchToken);
        log.info("found: {}", search.getNodeCount());
        search.getData().forEach(n -> {
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getNodeId());
            String wwwurl = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
            String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            Date created = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
            if (StringUtils.isNotBlank(wwwurl)) {
                log.info("removing empty property wwwurl for:" + name + " from:" + created);

                retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                    if (persistentMode) {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
                        nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
                        policyBehaviourFilter.enableBehaviour(nodeRef);
                    }
                    counter++;
                    return null;
                });
            }
        });
    }

    @Override
    public Class<?>[] getJobClasses() {
        this.addJobClass(FixEmptyWWWUrl.class);
        return super.allJobs;
    }

}
