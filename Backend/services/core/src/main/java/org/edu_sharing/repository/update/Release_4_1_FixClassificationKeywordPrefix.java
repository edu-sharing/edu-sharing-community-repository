package org.edu_sharing.repository.update;

import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;

import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@UpdateService
public class Release_4_1_FixClassificationKeywordPrefix {

    private final SearchService searchService;
    private final NodeService nodeService;

    @Autowired
    public Release_4_1_FixClassificationKeywordPrefix(SearchService ss, NodeService ns) {
        this.searchService = ss;
        this.nodeService = ns;
    }

    @UpdateRoutine(
            id = "Release_4_1_FixClassificationKeywordPrefix",
            description = "Fix for io's that got an cclom:classification_keyword property which is not defined as valid io prop.",
            order = 4100
    )
    public void execute() {
        RunAsWork<Void> runAs = () -> {

            SearchToken searchToken = new SearchToken();
            searchToken.setFrom(0);
            searchToken.setMaxResult(Integer.MAX_VALUE);
            searchToken.setElasticQuery(QueryBuilders.bool()
                    .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                    .must(m -> m.wildcard(w -> w.field("properties.cclom:classification_keyword").value("*")))
                    .build());
            org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();
            SearchResultNodeRef result = searchService.search(searchToken);
            result.getData().forEach(n -> {
                NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId());
                List<String> lomClassificationKeyword = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
                log.info("switching cclom:classification_keyword for " + nodeRef + " " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME)));
                if (lomClassificationKeyword != null && lomClassificationKeyword.size() > 0) {
                    nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD), (ArrayList<String>) lomClassificationKeyword);
                    nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
                }
            });
            return null;
        };

        AuthenticationUtil.runAsSystem(runAs);
    }
}
