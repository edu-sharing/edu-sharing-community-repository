package org.edu_sharing.service.monitoring;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.hazelcast.core.HazelcastInstance;
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
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.context.ApplicationContext;

public class Monitoring {

	private final ApplicationContext applicationContext;
	Logger logger = Logger.getLogger(Monitoring.class);
	
	final ServiceRegistry serviceRegistry;
	final Repository repositoryHelper;

	public static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public static enum Modes{
		SEARCH,
		SERVICE
	};
	
	public Monitoring() {
		applicationContext = AlfAppContextGate.getApplicationContext();
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
				SearchResultNodeRef search = SearchServiceFactory.getLocalService().search(searchToken);
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
	public boolean clusterCheckTimeout(int timeoutInSeconds) throws Throwable{
		Callable<Boolean> task = () -> {
            HazelcastInstance hz = applicationContext.getBean(HazelcastInstance.class);
            if(!hz.getLifecycleService().isRunning()) {
                logger.error("Hazelcast instance is down");
                return false;
            }
            return true;
        };
		return executeTask(timeoutInSeconds, task);
	}

	private<T> T executeTask(int timeoutInSeconds, Callable<T> task) throws Throwable{

		Future<T> future = executorService.submit(task);
		T result = future.get(timeoutInSeconds, TimeUnit.SECONDS);
		return result;
	}
	

}
