package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
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

import java.util.Date;

@JobDescription(description = "removes ccm:wwwurl property from nodes when value is an empty string")
public class FixEmptyWWWUrl extends AbstractJobMapAnnotationParams{
	
	Logger logger = Logger.getLogger(FixEmptyWWWUrl.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	NodeService nodeService = serviceRegistry.getNodeService();

	org.edu_sharing.service.search.SearchService searchService = SearchServiceFactory.getLocalService();

	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");

	@JobFieldDescription(description = "if false job just logs which nodes are found and would be updated")
	boolean persistentMode = true;
	
	int counter = 0;

	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				process();
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
		logger.info("counter: " + counter);
	}

	
	private void process(){

		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(0);
		searchToken.setMaxResult(Integer.MAX_VALUE);
		searchToken.setElasticQuery(QueryBuilders.bool()
						.must(m -> m.exists(e -> e.field("properties.ccm:wwwurl")))
						.must(m -> m.term(t -> t.field("properties.ccm:wwwurl.keyword").value("")))
				.build());

		SearchResultNodeRef search = searchService.search(searchToken);
		logger.info("found: " + search.getNodeCount());
		search.getData().forEach(n -> {
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getNodeId());
			String wwwurl = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
			String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			Date created = (Date)nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
			if(wwwurl != null && wwwurl.trim().equals("")) {
				logger.info("removing empty property wwwurl for:" + name + " from:" + created );

				serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
					@Override
					public Void execute() throws Throwable {
						if(persistentMode) {
							policyBehaviourFilter.disableBehaviour(nodeRef);
							nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
							policyBehaviourFilter.enableBehaviour(nodeRef);
						}
						counter++;
						return null;
					}
				});
			}
		});
	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(FixEmptyWWWUrl.class);
		return super.allJobs;
	}

}
