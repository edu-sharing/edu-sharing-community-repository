package org.edu_sharing.repository.server.jobs.quartz;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;


public class RestoreNodesBySolrQuery extends AbstractJob{

    public static final String PARAM_QUERY = "QUERY";
    public static final String PARAM_EXECUTE = "EXECUTE";
    public static final String PARAM_FALLBACK_FOLDER = "FALLBACK";

    public static String DESCRIPTION = "takes a solrquery, executed over archive store, tries to restore nodes.";


    int PAGE_SIZE = 100;

    Logger logger = Logger.getLogger(RestoreNodesBySolrQuery.class);

    ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String query = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_QUERY);
        Boolean execute = new Boolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
        String restoreFolderFallback = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_FALLBACK_FOLDER);
        NodeRef restoreFolderFallbackNodeRef = (restoreFolderFallback != null) ? new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,restoreFolderFallback) : null;
        if(query == null || query.trim().equals("")){
            logger.error("No "+PARAM_QUERY+" provided");
            return;
        }
        AuthenticationUtil.runAsSystem(() -> {
            run(query, execute, restoreFolderFallbackNodeRef);
            return null;
        });
    }

    public void run(String query, boolean execute, NodeRef restoreFolderFallback){
        logger.info("using query:" + query);
        ResultSet rs = null;
        int skipCount = 0;
        List<NodeRef> toRestore = new ArrayList<>();
        do{
            SearchParameters sp = new SearchParameters();
            sp.setQuery(query);
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.addStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
            sp.setMaxItems(PAGE_SIZE);
            sp.setSkipCount(skipCount);
            rs = serviceRegistry.getSearchService().query(sp);
            logger.info("found "+ rs.getNumberFound() +" to restore. skipCount:" + skipCount+". currentPageSize: "+rs.length());
            toRestore.addAll(rs.getNodeRefs());
            skipCount = skipCount + PAGE_SIZE;
        }while( rs.getNumberFound() > skipCount);
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
        this.addJobClass(RestoreNodesBySolrQuery.class);
        return super.allJobs;
    }
}
