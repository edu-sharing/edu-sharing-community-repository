package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Arrays;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class MigrationJobAuthorityStats extends AbstractJob {

	Logger logger = Logger.getLogger(MigrationJobAuthorityStats.class);
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
					
				try {
					RepositoryDao repoDao = RepositoryDao.getRepository("-home-");
		
					Set<String> authorities = serviceRegistry.getAuthorityService()
							.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
					logger.info(";authorities:" + authorities.size());
					for (String authority : authorities) {
						String authorityDN = serviceRegistry.getAuthorityService().getAuthorityDisplayName(authority);
						
						SearchToken token = new SearchToken();
						token.setFrom(0);
						token.setMaxResult(10);
						token.setContentType(ContentType.ALL);
						token.setLuceneString("@cm\\:name:\"*\"");
						token.disableSearchCriterias();
						token.setAuthorityScope(Arrays.asList(new String[] { authority }));
						NodeSearch search = NodeDao.search(repoDao, token, false);
						logger.info(";found "+ search.getCount() +";" + authority + ";" + authorityDN);
						
					}
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			
			
			return null;
				
			}
			
			
		};
		
		AuthenticationUtil.runAs(runAs,"admin");
	}

	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		super.addJobClass(MigrationJobAuthorityStats.class);
		return super.allJobs;
	}

}
