package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * {"LUCENE_QUERY":"@cm\\:edu_metadataset:\"mds_oeh\" AND ISNOTNULL:\"ccm:wwwurl\" AND -ASPECT:\"ccm:collection_io_reference\" AND PARENT:\"workspace://SpacesStore/b462fb4f-824b-47df-917c-3890f7e136da\"","MIME_TYPE":"text/plain","EXECUTE":"true"}
 */
public class FixMimeTypeJob extends AbstractJob {

    public static final String PARAM_LUCENE_QUERY = "LUCENE_QUERY";
    public static final String PARAM_MIMETYPE = "MIME_TYPE";
    public static final String PARAM_EXECUTE = "EXECUTE";

    Logger logger = Logger.getLogger(FixMimeTypeJob.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    SearchService searchService = serviceRegistry.getSearchService();
    NodeService nodeService = serviceRegistry.getNodeService();
    ContentService contentService = serviceRegistry.getContentService();

    private static final int PAGE_SIZE = 1000;
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String luceneQuery = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_LUCENE_QUERY);
        String mimeType = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_MIMETYPE);

        if (luceneQuery == null || luceneQuery.trim().equals("")) {
            logger.error("missing " + PARAM_LUCENE_QUERY);
            return;
        }

        if (mimeType == null || mimeType.trim().equals("")) {
            logger.error("missing " + PARAM_MIMETYPE);
            return;
        }

        Boolean execute = new Boolean((String) jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));

        AuthenticationUtil.runAsSystem(()->{
            Set<NodeRef> collect = new HashSet<>();
            execute(0,luceneQuery, collect);
            fix(collect,execute,mimeType);
            return null;
        });

    }

    private void execute(int page, String query, Set<NodeRef> collect) {
        logger.info("page:" + page);
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        sp.setSkipCount(page);
        sp.setMaxItems(PAGE_SIZE);

        sp.setQuery(query);

        logger.info("query:" + sp.getQuery());
        ResultSet resultSet = searchService.query(sp);

        for (NodeRef nodeRef : resultSet.getNodeRefs()) {
            collect.add(nodeRef);
        }
        if(resultSet.hasMore()) {
            execute(page + PAGE_SIZE,query,collect);
        }
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
