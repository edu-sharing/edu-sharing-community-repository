package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.List;

@JobDescription(description = "removes ALL_AUTHORITIES permission from io's and map's")
public class TakeBackGlobalShare extends AbstractJobMapAnnotationParams {
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private NodeService nodeService = serviceRegistry.getNodeService();
	private PermissionService permissionService = serviceRegistry.getPermissionService();
	
	Logger logger = Logger.getLogger(TakeBackGlobalShare.class);

	@JobFieldDescription(description = "if false job runs in protocol mode")
	Boolean execute;

	TakeBackGlobalShareWorker worker;
	
	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		worker = new TakeBackGlobalShareWorker(nodeService, permissionService, execute);
		if(execute == null) execute = Boolean.FALSE;

		AuthenticationUtil.RunAsWork<Void> runAs = () -> {
            run();
            return null;
        };
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	private void run() {

		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(0);
		searchToken.setMaxResult(Integer.MAX_VALUE);
		searchToken.setAuthorityScope(List.of(PermissionService.ALL_AUTHORITIES));
		searchToken.setElasticQuery(QueryBuilders.bool()
				.minimumShouldMatch("1")
				.should(s -> s.term(t -> t.field("type").value("ccm:io")))
				.should(s -> s.term(t -> t.field("type").value("ccm:map")))
				.build());
		SearchService searchService = SearchServiceFactory.getLocalService();
		SearchResultNodeRef search = searchService.search(searchToken);
		search.getData().forEach(n -> {
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,n.getNodeId());
			worker.work(nodeRef);
		});
	}

	@Override
	public Class[] getJobClasses() {
		this.addJobClass(TakeBackGlobalShare.class);
		return this.allJobs;
	}

}
