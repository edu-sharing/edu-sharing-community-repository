package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class TakeBackGlobalShare extends AbstractJob {
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private NodeService nodeService = serviceRegistry.getNodeService();
	private SearchService searchService = serviceRegistry.getSearchService();
	private PermissionService permissionService = serviceRegistry.getPermissionService();
	
	Logger logger = Logger.getLogger(TakeBackGlobalShare.class);
	
	public static String PARAM_EXECUTE = "EXECUTE";
	
	int PAGE_SIZE = 100;
	
	TakeBackGlobalShareWorker worker;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		boolean execute = new Boolean((String)context.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
		
		worker = new TakeBackGlobalShareWorker(nodeService, permissionService, execute);
		
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run(execute, 0);
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	private void run(boolean execute, int page) {
		ESSearchParameters sp = new ESSearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setSkipCount(page);
		sp.setMaxItems(PAGE_SIZE);
		sp.setAuthorities(new String[] {PermissionService.ALL_AUTHORITIES});
		sp.setQuery("TYPE:\"ccm:io\" OR TYPE:\"ccm:map\"");
		
		ResultSet resultSet = searchService.query(sp);
		logger.info("page " + page + " from " + resultSet.getNumberFound());
		
		for(NodeRef nodeRef : resultSet.getNodeRefs()) {
			worker.work(nodeRef);
		}
		
		if(resultSet.hasMore()) {
			run(execute, page + PAGE_SIZE);
		}
	}

	@Override
	public Class[] getJobClasses() {
		this.addJobClass(TakeBackGlobalShare.class);
		return this.allJobs;
	}

}
