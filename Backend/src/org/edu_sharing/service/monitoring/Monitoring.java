package org.edu_sharing.service.monitoring;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

public class Monitoring {
	
	Logger logger = Logger.getLogger(Monitoring.class);
	
	ServiceRegistry serviceRegistry;
	Repository repositoryHelper;
	
	public static enum Modes{
		SEARCH,
		SERVICE
	};
	
	public Monitoring() {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
	}
	/**
	 * checks alfresco services to find out database problems
	 * 
	 * @return
	 */
	public String alfrescoServicesCheck() {
		RunAsWork<String> runAs = new RunAsWork<String>() {
			
			@Override
			public String doWork() throws Exception {
				return repositoryHelper.getCompanyHome().getId();
			}
		};
		return AuthenticationUtil.runAsSystem(runAs);
	}
	
	public String alfrescoSearchEngineCheck() {
		RunAsWork<String> runAs = new RunAsWork<String>() {
			
			@Override
			public String doWork() throws Exception {
				String nodeId = repositoryHelper.getCompanyHome().getId();
				SearchParameters sp = new SearchParameters();
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.setSkipCount(0);
				sp.setMaxItems(1);
				sp.setQuery("@sys\\:node-uuid:\"" + nodeId + "\"");
				ResultSet rs = serviceRegistry.getSearchService().query(sp);
				return rs.getNodeRefs().get(0).getId();
			}
		};
		return AuthenticationUtil.runAsSystem(runAs);
	}
	
	public String alfrescoServicesCheckTimeout(int timeoutInSeconds) throws Throwable{
		Callable<String> task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return alfrescoServicesCheck();
			}
		};
		return executeTask(timeoutInSeconds, task);
	}
	
	public String alfrescoSearchEngineCheckTimeout(int timeoutInSeconds) throws Throwable{
		Callable<String> task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return alfrescoSearchEngineCheck();
			}
		};
		return executeTask(timeoutInSeconds, task);
	}
	
	private String executeTask(int timeoutInSeconds, Callable<String> task) throws Throwable{
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> future = executor.submit(task);
		String result = future.get(timeoutInSeconds, TimeUnit.SECONDS);
		return result;
	}
	

}
