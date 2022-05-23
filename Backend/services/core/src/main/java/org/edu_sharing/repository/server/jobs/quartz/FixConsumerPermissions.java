package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FixConsumerPermissions extends AbstractJob {

    public static final String PARAM_ROOT_NODE = "ROOT_NODE";
    Logger logger = Logger.getLogger(FixConsumerPermissions.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    SearchService searchService = serviceRegistry.getSearchService();

    NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");

    BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");

    private static final int PAGE_SIZE = 100;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                execute();
                return null;
            }
        };
        AuthenticationUtil.runAsSystem(runAs);


    }

    private void execute() {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        sp.setPermissionEvaluation(PermissionEvaluationMode.NONE);
        sp.setMaxItems(Integer.MAX_VALUE);
        // WARNING: Also fetches TOOLPERMISSIONS since they are sub-objects of io!
        sp.setQuery("SELECT cmis:objectId FROM ccm:io");
        long time=System.currentTimeMillis();
        ResultSet resultSet = searchService.query(sp);
        for(NodeRef nodeRef : resultSet.getNodeRefs()) {
            //if(!nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO)))
                //logger.warn(nodeRef+": "+nodeService.getType(nodeRef));
        }
        logger.info("query time "+(System.currentTimeMillis()-time)+"ms");
        logger.info( resultSet.getNodeRefs().size());
    }

    @Override
    public Class[] getJobClasses() {
        super.addJobClass(FixConsumerPermissions.class);
        return allJobs;
    }
}
