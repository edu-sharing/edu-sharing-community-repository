package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.helper.NodeHelper;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.camel.model.rest.RestParamType.query;

@JobDescription(description = "logs diff elastic and db")
public class ProtocolDiffDBSolrJob extends AbstractJobMapAnnotationParams{

    public static final String DESCRIPTION = "logs diff solr and db";
    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    Logger logger = Logger.getLogger(ProtocolDiffDBSolrJob.class);

    @JobFieldDescription(description = "folder that needs to be compared")
    String startFolder;

    int PAGE_SIZE = 1000;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if(startFolder == null){
            logger.error("no startFolder provided");
            return;
        }
        AuthenticationUtil.runAsSystem(() -> {
            run(startFolder);
            return null;
        });
    }

    private void run(String startFolder){
        Path path = serviceRegistry.getNodeService().getPath(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder));

        String pathPrefixString = path.toPrefixString(serviceRegistry.getNamespaceService());
        logger.info("pathPrefixString:"+pathPrefixString);
        pathPrefixString = pathPrefixString.replaceFirst("/","");
        pathPrefixString = pathPrefixString+"*";
        String pathQuery = pathPrefixString;


        SearchToken token = new SearchToken();
        token.setFrom(0);
        token.setMaxResult(Integer.MAX_VALUE);
        token.setElasticQuery(
            QueryBuilders.bool()
                    .must(m -> m.wildcard(w -> w.field("fulldisplaypath").value(pathQuery)))
                    .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                    .build());

        org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();

        logger.info("collect elastic nodes " + token.getElasticQuery());
        SearchResultNodeRef search = searchService.search(token);
        List<NodeRef> nodesInElastic = search.getData().stream()
                .map(n -> new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId()))
                .collect(Collectors.toList());

        logger.info("collection db nodes");
        List<NodeRef> nodesInDb = new NodeHelper().getNodes(startFolder);
        List<NodeRef> diff = new ArrayList<>(nodesInDb);
        diff.removeAll(nodesInElastic);
        if(diff.size() == 0){
            logger.info("no diff between database and elastic in this folder " + startFolder);
        }
        for(NodeRef node : diff){
            logger.info("in db not in elastic:"+node);
        }
    }
}
