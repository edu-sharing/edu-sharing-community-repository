package org.edu_sharing.repository.server.importer;

import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class FactualTermElasticUpdater {

    private final NodeService nodeService;

    public void touchNodes(String factualTermIdent){
        Map<String,Object> filter = new HashMap<>();
        filter.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD, factualTermIdent);
        List<NodeRef> nodeRefs = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,filter, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        nodeRefs.forEach(nodeRef -> nodeService.touch(nodeRef.getId(), true));
    }
}
