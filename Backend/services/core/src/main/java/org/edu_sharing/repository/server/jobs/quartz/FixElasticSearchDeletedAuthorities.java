package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.Map;

@JobDescription(description = "checks for authorities (users/groups) removed in repository but still existing in the elasticsearch authorities index. please check that tracker is 100% finished and tracker is disabled before running this job.")
public class FixElasticSearchDeletedAuthorities extends FixElasticSearchBase {

    @JobFieldDescription(description = "if false (default) no changes will be done.")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of authorities that have to be checked. optional. if not set all authorities will be searched.", sampleValue = "{\"term\":{\"type\":\"cm:person\"}}")
    protected String query;

    Logger logger = Logger.getLogger(FixElasticSearchDeletedAuthorities.class);

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        AuthenticationUtil.runAsSystem(() -> {
            try {
                Query.Builder builder = getBuilder(query);
                search(SearchServiceElastic.AUTHORITIES_INDEX, builder.build(), new DeletedAuthoritiesHandler());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        });
    }

    public class DeletedAuthoritiesHandler implements SearchResultHandler {
        @Override
        public void handleSearchHit(Hit<Map> searchHit) throws IOException {

            String dbid = searchHit.id();

            String type = (String) searchHit.source().get("type");
            Map properties = (Map) searchHit.source().get("properties");
            String authorityName = null;
            if (properties != null) {
                authorityName = (String) properties.get("cm:authorityName");
                if (authorityName == null) {
                    authorityName = (String) properties.get("cm:userName");
                }
            }

            NodeRef alfNodeRef = getNodeRef(searchHit);
            if (!serviceRegistry.getNodeService().exists(alfNodeRef)) {
                logger.info(alfNodeRef + ";dbid:" + dbid + ";type:" + type + ";authority:" + authorityName + ";does not longer exist in repo. will remove.");

                if (execute) {
                    searchServiceElastic.deleteNative(DeleteRequest.of(req -> req
                            .index(SearchServiceElastic.AUTHORITIES_INDEX)
                            .id(dbid)));
                }
            }
        }
    }
}
