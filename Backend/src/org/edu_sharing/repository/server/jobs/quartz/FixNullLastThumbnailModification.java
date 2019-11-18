package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class FixNullLastThumbnailModification extends AbstractJob {
	
	Logger logger = Logger.getLogger(FixNullLastThumbnailModification.class);
	
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
				execute(0, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				logger.info("counter: " + counter +" for " + StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				counter= 0;
				execute(0, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
				logger.info("counter: " + counter +" for " + StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
		
	}
	
	private void execute(int page, StoreRef storeRef){
		logger.info("page:" + page);
		SearchParameters sp = new SearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(storeRef);
		sp.setSkipCount(page);
		sp.setMaxItems(PAGE_SIZE);
		
		sp.setQuery("ASPECT:\"cm:thumbnailModification\" AND ISUNSET:\"cm:lastThumbnailModification\"");
		
		logger.info("query:" + sp.getQuery());
		ResultSet resultSet = searchService.query(sp);
		
		for(NodeRef nodeRef : resultSet.getNodeRefs()) {
			List<String> thumbnailMods = (List<String>) nodeService.getProperty(nodeRef, ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA);
			String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			Date created = (Date)nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
			if(thumbnailMods == null) {
				logger.info("setting cm:lastThumbnailModification for:" + name + " from:" + created );
				
				serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
					@Override
					public Void execute() throws Throwable {
						if(perisistentMode) {
							policyBehaviourFilter.disableBehaviour(nodeRef);
							nodeService.setProperty(nodeRef, ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA, new ArrayList());
							policyBehaviourFilter.enableBehaviour(nodeRef);
						}
						counter++;
						return null;
					}
				});
			}
		}
		
		if(resultSet.hasMore()) {
			execute(page + PAGE_SIZE, storeRef);
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(FixNullLastThumbnailModification.class);
		return super.allJobs;
	}

}
