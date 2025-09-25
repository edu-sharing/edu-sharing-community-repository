package org.edu_sharing.service.monitoring;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.context.ApplicationContext;

public class Monitoring {
	
	Logger logger = Logger.getLogger(Monitoring.class);
	
	ServiceRegistry serviceRegistry;
	Repository repositoryHelper;

	public static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
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
				//maybe do only ping here
				SearchToken searchToken = new SearchToken();
				searchToken.setElasticQuery(QueryBuilders.term().field("type").value("ccm:io").build());
				searchToken.setFrom(0);
				searchToken.setMaxResult(1);
				SearchResultNodeRef search = SearchServiceFactory.getInstance().getLocalService().search(searchToken);
				if(search != null && search.getData() != null && search.getData().size() > 0) {
					return search.getData().get(0).getNodeId();
				}else return null;
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

		Future<String> future = executorService.submit(task);
		String result = future.get(timeoutInSeconds, TimeUnit.SECONDS);
		return result;
	}
	

}
