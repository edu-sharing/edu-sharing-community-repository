package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.text.StringEscapeUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public abstract class FixElasticSearchBase extends AbstractJobMapAnnotationParams {

    protected final ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    protected final ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
    protected final BehaviourFilter policyBehaviourFilter = applicationContext.getBean("policyBehaviourFilter", BehaviourFilter.class);
    protected final NodeService nodeService = serviceRegistry.getNodeService();

    static int pageSize = 1000;
    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    public interface SearchResultHandler {
        public void handleSearchHit(Hit<Map> searchHit) throws IOException;
    }

    protected Query. Builder getBuilder(String query) {
        Query.Builder builder = new Query.Builder();
        if (query == null) {
            builder.bool(b -> b.must(q -> q.matchAll(all -> all)));
        } else {
            String unescapedQuery = StringEscapeUtils.unescapeJson(query);
            logger.info("query:" + unescapedQuery);
            builder.wrapper(w -> w.query(Base64.getEncoder().encodeToString(unescapedQuery.getBytes())));
        }
        return builder;
    }

    protected void search(Query query, SearchResultHandler searchResultHandler) throws IOException {
        search(SearchServiceElastic.WORKSPACE_INDEX, query, searchResultHandler);
    }

    protected void search(String index, Query query, SearchResultHandler searchResultHandler) throws IOException {
        logger.info("search on index " + index + " with handler: " + searchResultHandler.getClass().getName());

        Time scroll = Time.of(time -> time.time("4h"));
        ResponseBody<Map> response = null;
        int page = 0;
        do {
            if (response == null) {
                response = search(index, query, scroll);
            } else {
                response = scroll(scroll, response.scrollId());
            }
            HitsMetadata<Map> searchHits = response.hits();
            if(searchHits.hits().size() == 0) break;
            logger.info("page:" + page + " with result size:" + searchHits.hits().size() + " of:" + searchHits.total().value());
            for (Hit<Map> searchHit : searchHits.hits()) {
                searchResultHandler.handleSearchHit(searchHit);
            }
            page++;
        } while (response.hits() != null && !response.hits().hits().isEmpty());

        boolean clearSuccess = clearScroll(response.scrollId());
        if (clearSuccess) logger.info("cleared scroll successfully");
        else logger.error("clear of scroll " + response.scrollId() + " failed");
    }

    protected NodeRef getNodeRef(Hit<Map> searchHit){
        Map nodeRef = (Map) searchHit.source().get("nodeRef");
        String nodeId = (String) nodeRef.get("id");
        Map storeRef = (Map) nodeRef.get("storeRef");
        String protocol = (String) storeRef.get("protocol");
        String identifier = (String) storeRef.get("identifier");

        return new NodeRef(protocol, identifier, nodeId);
    }


    protected SearchResponse<Map> search(String index, Query query, Time scroll) throws IOException {
        return searchServiceElastic.searchNative(SearchRequest.of(req -> req
                .index(index)
                .size(pageSize)
                .source(src->src.filter(filter->filter.excludes("preview", "content")))
                .scroll(scroll)
                .query(query)));
    }

    protected ScrollResponse<Map> scroll(Time scroll, String scrollId) throws IOException {
        return searchServiceElastic.scrollNative(ScrollRequest.of(sq -> sq
                .scrollId(scrollId)
                .scroll(scroll)));
    }

    protected boolean clearScroll(String scrollId) throws IOException {
        ClearScrollResponse clearScrollResponse = searchServiceElastic.clearScrollNative(ClearScrollRequest.of(req -> req.scrollId(scrollId)));
        return clearScrollResponse.succeeded();
    }
}
