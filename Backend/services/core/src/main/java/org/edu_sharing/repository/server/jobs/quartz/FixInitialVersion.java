package org.edu_sharing.repository.server.jobs.quartz;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.cache.RepositoryCacheTool;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

@JobDescription(description = "create initial version for ccm:io nodes if they don't exist")
public class FixInitialVersion extends AbstractJobMapAnnotationParams{

	Logger logger = Logger.getLogger(FixInitialVersion.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	VersionService versionService = serviceRegistry.getVersionService();

	org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();
	
	NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");

	@JobFieldDescription(description = "if false job just logs which nodes are found and would be updated")
	boolean persistentMode = false;

	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		AuthenticationUtil.RunAsWork<Void> runAs = () -> {
            process();
            return null;
        };
		AuthenticationUtil.runAsSystem(runAs);
		
		
	}
	
	private void process() {

		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(0);
		searchToken.setMaxResult(Integer.MAX_VALUE);
		searchToken.setElasticQuery(QueryBuilders.bool()
				.mustNot(m -> m.exists(e -> e.field("properties.cclom:version")))
				.must(m -> m.term(t -> t.field("type").value("ccm:io")))
				.build());

		SearchResultNodeRef search = searchService.search(searchToken);
		logger.info("found: " + search.getNodeCount());
		search.getData().forEach(n -> {
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getNodeId());
			VersionHistory vh = versionService.getVersionHistory(nodeRef);
			if(vh == null) {
				logger.info("creating initial version for:" + nodeRef +"  " + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));

				Map<String, Serializable> transFormedProps = transformQNameKeyToString(nodeService.getProperties(nodeRef));
				transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);

				serviceRegistry.getRetryingTransactionHelper().doInTransaction((RetryingTransactionCallback<Void>) () -> {
                    if(persistentMode) {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
						Version version = versionService.createVersion(nodeRef, transFormedProps);
						this.nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION), version.getVersionLabel());
						new RepositoryCache().remove(nodeRef.getId());
						policyBehaviourFilter.enableBehaviour(nodeRef);
                        return null;
                    }
                    return null;
                });
			}
		});
	}
	
	Map<String,Serializable> transformQNameKeyToString(Map<QName, Serializable> props){
		Map<String,Serializable> result = new HashMap<>();
		for(Map.Entry<QName,Serializable> entry : props.entrySet()){
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}
	
	
	@Override
	public Class[] getJobClasses() {
		super.addJobClass(FixInitialVersion.class);
		return allJobs;
	}
}
