package org.edu_sharing.repository.server.jobs.helper;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.edu_sharing.alfresco.service.search.cmis.Query;
import org.edu_sharing.alfresco.service.search.cmis.QueryBuilder;
import org.edu_sharing.alfresco.service.search.cmis.QueryStatement;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects nodes by aspect using a CMIS/TMDQ query (executed directly against the database, no
 * Solr/Elasticsearch involved). Used as an alternative to recursively traversing the whole
 * repository tree when the nodes to process can be identified by a marker aspect instead.
 * <p>
 * Runs one query per combination of store, type and aspect (Alfresco's TMDQ engine supports only a
 * single store per query, and {@link QueryBuilder} joins multiple aspects as AND, not OR - see
 * {@code QueryBuilder.addAspect}). Each query intentionally avoids the usual skip/maxItems paging
 * loop: with {@code maxItems=-1}, Alfresco's {@code DBQueryEngine} streams the whole result set in a
 * single pass instead of re-scanning from offset 0 for every page, which would otherwise turn a
 * large result set into an O(n^2) collection.
 */
@Slf4j
public class NodeCollectorCmis {

    private final List<String> aspects;
    private final List<StoreRef> stores;
    private final List<String> types;

    private final QueryBuilder queryBuilder;
    private final SearchService searchService;

    public NodeCollectorCmis(List<String> aspects, List<StoreRef> stores, List<String> types) {
        this.aspects = aspects;
        this.stores = stores;
        this.types = types;

        this.queryBuilder = ApplicationContextFactory.getApplicationContext().getBean(QueryBuilder.class);
        this.searchService = ApplicationContextFactory.getApplicationContext().getBean("SearchService", SearchService.class);
    }

    public List<NodeRef> getNodes() {
        Set<NodeRef> result = new LinkedHashSet<>();
        for (StoreRef store : stores) {
            for (String type : types) {
                for (String aspect : aspects) {
                    List<NodeRef> found = query(store, type, aspect);
                    log.info("collected {} node(s) for store={} type={} aspect={}", found.size(), store, type, aspect);
                    result.addAll(found);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<NodeRef> query(StoreRef store, String type, String aspect) {
        QueryStatement query = Query.select(CCConstants.SYS_PROP_NODE_UID)
                .from(type)
                .hasAspect(aspect);
        String cmisQuery = queryBuilder.build(query);

        SearchParameters params = new SearchParameters();
        params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        params.addStore(store);
        params.setQuery(cmisQuery);
        // no paging: a single streaming pass instead of re-scanning from offset 0 per page
        params.setMaxItems(-1);
        params.setSkipCount(0);
        // runs as system, so no real ACL evaluation happens anyway - disable the safety limits
        // that would otherwise silently truncate large result sets
        params.setMaxPermissionChecks(Integer.MAX_VALUE);
        params.setMaxPermissionCheckTimeMillis(Long.MAX_VALUE);

        ResultSet rs = searchService.query(params);
        try {
            return rs.getNodeRefs();
        } finally {
            rs.close();
        }
    }
}
