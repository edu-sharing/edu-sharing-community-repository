package org.edu_sharing.repository.server.jobs.quartz;


import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.model.ContentModel;
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
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@JobDescription(description = "takes a elastic query, executed over archive store, tries to restore nodes.")
public class RestoreNodesByQuery extends AbstractJobMapAnnotationParams {

    @JobFieldDescription(description = "elastic query",sampleValue = "{\"term\":{\"properties.cm:name\":\"test\"}}")
    String query;

    @JobFieldDescription(description = "if false job runs in protocol mode")
    Boolean execute;

    @JobFieldDescription(description = "nodeId of a folder. when a node can not be restored in origin folder cause of duplicate exception. this folder is used.")
    String restoreFolderFallback;

    Logger logger = Logger.getLogger(RestoreNodesByQuery.class);

    ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        NodeRef restoreFolderFallbackNodeRef = (restoreFolderFallback != null) ? new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,restoreFolderFallback) : null;
        if(query == null || query.trim().equals("")){
            logger.error("No query provided");
            return;
        }
        if(execute == null){
            execute = Boolean.FALSE;
        }
        AuthenticationUtil.runAsSystem(() -> {
            run(query, execute, restoreFolderFallbackNodeRef);
            return null;
        });
    }

    public void run(String query, boolean execute, NodeRef restoreFolderFallback){
        logger.info("using query:" + query);

        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(Integer.MAX_VALUE);
        searchToken.setStoreName(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getIdentifier());
        searchToken.setStoreProtocol(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getProtocol());
        searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(query.getBytes()))).build());

        org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();
        SearchResultNodeRef result = searchService.search(searchToken);
        logger.info("found "+ result.getNodeCount() +" to restore.");
        List<NodeRef> toRestore = result.getData().stream()
                .map(n -> new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId()))
                .collect(Collectors.toList());
        restore(toRestore, execute, restoreFolderFallback);
    }

    public void restore(List<NodeRef> toRestore, boolean execute, NodeRef restoreFolderFallback){
        for(NodeRef nodeRef : toRestore){
            ChildAssociationRef childRef = (ChildAssociationRef)nodeService.getProperty(nodeRef, ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
            if(childRef == null){
                logger.error("cannot restore "+nodeRef +" cause PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC is null");
                continue;
            }

            if(!nodeService.exists(nodeRef)){
                logger.error("cannot restore " + nodeRef+ " cause it does not exist. maybe already restored");
                continue;
            }

            if(!nodeService.exists(childRef.getParentRef())){
                logger.error("cannot restore "+nodeRef +" cause PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC noderef does not exist");
                continue;
            }



            String nodeName = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_NAME);
            String assocName = QName.createValidLocalName(nodeName);
            assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

            String restoreToName = (String)nodeService.getProperty(childRef.getParentRef(),ContentModel.PROP_NAME);

            try{
                logger.info("restoring node;"+nodeRef+";"+ nodeName + ";to;"+restoreToName );
                if(execute) {
                    nodeService.restoreNode(nodeRef, childRef.getParentRef(), childRef.getTypeQName(), QName.createQName(assocName));
                }
            }catch (DuplicateChildNodeNameException e){
                if(restoreFolderFallback == null){
                    logger.error("cannot restore cause of "+ e.getMessage()+" no fallback folder provided");
                }else{
                    if(!nodeService.exists(restoreFolderFallback)){
                        logger.error("cannot restore cause of "+ e.getMessage()+" fallback folder does not exist");
                        continue;
                    }
                    nodeService.restoreNode(nodeRef,restoreFolderFallback,childRef.getTypeQName(),QName.createQName(assocName));
                    logger.warn("node restored in fallback folder " + nodeRef +" fb:"+restoreFolderFallback +" cause of "+e.getMessage());
                }
            }
        }
    }

    @Override
    public Class[] getJobClasses() {
        this.addJobClass(RestoreNodesByQuery.class);
        return super.allJobs;
    }
}
