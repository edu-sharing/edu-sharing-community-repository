package org.edu_sharing.repository.server.jobs.quartz;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

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
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class FixEmptyWWWUrl extends AbstractJob {
	
	Logger logger = Logger.getLogger(FixEmptyWWWUrl.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	SearchService searchService = serviceRegistry.getSearchService();
	NodeService nodeService = serviceRegistry.getNodeService();
	
	private static final int PAGE_SIZE = 100;
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
	
	boolean perisistentMode = false;
	
	public static final String PARAM_PERSIST = "PERSIST";
	
	int counter = 0;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String persist = (String)context.getJobDetail().getJobDataMap().get(PARAM_PERSIST);
		perisistentMode = new Boolean(persist);
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				execute(0);
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
		logger.info("counter: " + counter);
	}
	
	private void execute(int page){
		logger.info("page:" + page);
		SearchParameters sp = new SearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setSkipCount(page);
		sp.setMaxItems(PAGE_SIZE);
		
		sp.setQuery("ISNOTNULL:\"ccm:wwwurl\"");
		
		logger.info("query:" + sp.getQuery());
		ResultSet resultSet = searchService.query(sp);
		
		for(NodeRef nodeRef : resultSet.getNodeRefs()) {
			
			String wwwurl = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
			String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			Date created = (Date)nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
			if(wwwurl != null && wwwurl.trim().equals("")) {
				logger.info("removing empty property wwwurl for:" + name + " from:" + created );
				
				serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
					@Override
					public Void execute() throws Throwable {
						if(perisistentMode) {
							policyBehaviourFilter.disableBehaviour(nodeRef);
							nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
							policyBehaviourFilter.enableBehaviour(nodeRef);
						}
						counter++;
						return null;
					}
				});
			}
		}
		
		if(resultSet.hasMore()) {
			execute(page + PAGE_SIZE);
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(FixEmptyWWWUrl.class);
		return super.allJobs;
	}

}
