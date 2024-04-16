package org.edu_sharing.repository.update;

import java.util.ArrayList;
import java.util.List;

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

import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
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
            SearchParameters sp = new SearchParameters();
            sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.setMaxItems(-1);
            sp.setQuery("TYPE:\"ccm:io\" AND @cclom\\:classification_keyword:\"*\"");

            ResultSet rs = searchService.query(sp);
            for (NodeRef nodeRef : rs.getNodeRefs()) {
                List<String> lomClassificationKeyword = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
                log.info("switching cclom:classification_keyword for " + nodeRef + " " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME)));
                if (lomClassificationKeyword != null && lomClassificationKeyword.size() > 0) {
                    nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD), (ArrayList<String>) lomClassificationKeyword);
                    nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
                }
            }
            return null;
        };

        AuthenticationUtil.runAsSystem(runAs);
    }
}
