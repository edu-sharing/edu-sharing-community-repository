package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "resets mimetype to a given value for nodes matching elastic query")
public class FixMimeTypeJob extends AbstractJobMapAnnotationParams {

    @JobFieldDescription(description = "node filter", sampleValue = "{\"term\": {\"properties.cclom:format\":\"text/xml\"}}")
    String filter;

    @JobFieldDescription(description = "new mimetype for the nodes")
    String mimeType;

    @JobFieldDescription(description = "if false job runs in protocol mode")
    Boolean execute;

    Logger logger = Logger.getLogger(FixMimeTypeJob.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    ContentService contentService = serviceRegistry.getContentService();

    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");


    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if (filter == null || filter.trim().equals("")) {
            logger.error("missing " + filter);
            return;
        }

        if (mimeType == null || mimeType.trim().equals("")) {
            logger.error("missing " + mimeType);
            return;
        }

        if(execute == null){
            execute = Boolean.FALSE;
        }

        AuthenticationUtil.runAsSystem(()->{
            SearchToken searchToken = new SearchToken();
            searchToken.setFrom(0);
            searchToken.setMaxResult(Integer.MAX_VALUE);
            searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(filter.getBytes()))).build());
            org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();
            SearchResultNodeRef search = searchService.search(searchToken);
            Set<NodeRef> collect = search.getData().stream()
                    .map(n -> new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId()))
                    .collect(Collectors.toSet());
            fix(collect,execute,mimeType);
            return null;
        });

    }


    public void fix(Set<NodeRef> nodeRefs, boolean execute, String mimeType){
        logger.info("fixing:" + nodeRefs.size());
        for(NodeRef nodeRef:nodeRefs){
            ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
            logger.info("fixing mimetype:" + reader.getMimetype()+ " to:" + mimeType +" "+ nodeRef);
            nodeRefs.add(nodeRef);
            if (execute) {
                serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                    try {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
                        ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
                        writer.setMimetype(mimeType);
                        writer.putContent(reader.getContentInputStream());
                        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), mimeType);
                        new RepositoryCache().remove(nodeRef.getId());
                    } finally {
                        policyBehaviourFilter.enableBehaviour(nodeRef);
                        return null;
                    }
                });
            }
        }
    }
}
