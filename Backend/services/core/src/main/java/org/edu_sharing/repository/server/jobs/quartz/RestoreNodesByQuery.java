package org.edu_sharing.repository.server.jobs.quartz;


import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.*;
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

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@JobDescription(description = "takes a elastic query, executed over archive store, tries to restore nodes.")
public class RestoreNodesByQuery extends AbstractJobMapAnnotationParams {

    @JobFieldDescription(description = "elastic query", sampleValue = "{\"term\":{\"properties.cm:name\":\"test\"}}")
    String query;

    @JobFieldDescription(description = "if false job runs in protocol mode")
    Boolean execute;

    @JobFieldDescription(description = "nodeId of a folder. when a node can not be restored in origin folder cause of duplicate exception. this folder is used.")
    String restoreFolderFallback;


    @Autowired
    private NodeService nodeService;

    @Autowired
    private org.edu_sharing.service.search.SearchService searchService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        NodeRef restoreFolderFallbackNodeRef = (restoreFolderFallback != null) ? new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, restoreFolderFallback) : null;
        if (StringUtils.isBlank(query)) {
            log.error("No query provided");
            return;
        }
        if (execute == null) {
            execute = Boolean.FALSE;
        }
        AuthenticationUtil.runAsSystem(() -> {
            run(query, execute, restoreFolderFallbackNodeRef);
            return null;
        });
    }

    public void run(String query, boolean execute, NodeRef restoreFolderFallback) {
        log.info("using query:{}", query);

        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(Integer.MAX_VALUE);
        searchToken.setStoreName(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getIdentifier());
        searchToken.setStoreProtocol(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getProtocol());
        searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(query.getBytes()))).build());

        SearchResultNodeRef result = searchService.search(searchToken);
        log.info("found {} to restore.", result.getNodeCount());
        List<NodeRef> toRestore = result.getData().stream()
                .map(n -> new NodeRef(new StoreRef(n.getStoreProtocol(), n.getStoreId()), n.getNodeId()))
                .collect(Collectors.toList());
        restore(toRestore, execute, restoreFolderFallback);
    }

    public void restore(List<NodeRef> toRestore, boolean execute, NodeRef restoreFolderFallback) {
        for (NodeRef nodeRef : toRestore) {
            ChildAssociationRef childRef = (ChildAssociationRef) nodeService.getProperty(nodeRef, ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
            if (childRef == null) {
                log.error("cannot restore {} cause PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC is null", nodeRef);
                continue;
            }

            if (!nodeService.exists(nodeRef)) {
                log.error("cannot restore {} cause it does not exist. maybe already restored", nodeRef);
                continue;
            }

            if (!nodeService.exists(childRef.getParentRef())) {
                log.error("cannot restore {} cause PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC noderef does not exist", nodeRef);
                continue;
            }


            String nodeName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            String assocName = QName.createValidLocalName(nodeName);
            assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

            String restoreToName = (String) nodeService.getProperty(childRef.getParentRef(), ContentModel.PROP_NAME);

            try {
                log.info("restoring node;{};{};to;{}", nodeRef, nodeName, restoreToName);
                if (execute) {
                    nodeService.restoreNode(nodeRef, childRef.getParentRef(), childRef.getTypeQName(), QName.createQName(assocName));
                }
            } catch (DuplicateChildNodeNameException e) {
                if (restoreFolderFallback == null) {
                    log.error("cannot restore cause of {} no fallback folder provided", e.getMessage());
                } else {
                    if (!nodeService.exists(restoreFolderFallback)) {
                        log.error("cannot restore cause of {} fallback folder does not exist", e.getMessage());
                        continue;
                    }
                    nodeService.restoreNode(nodeRef, restoreFolderFallback, childRef.getTypeQName(), QName.createQName(assocName));
                    log.warn("node restored in fallback folder {} fb:{} cause of {}", nodeRef, restoreFolderFallback, e.getMessage());
                }
            }
        }
    }

    @Override
    public Class<?>[] getJobClasses() {
        this.addJobClass(RestoreNodesByQuery.class);
        return super.allJobs;
    }
}
