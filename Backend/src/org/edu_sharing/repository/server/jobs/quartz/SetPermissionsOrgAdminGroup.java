package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Arrays;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class SetPermissionsOrgAdminGroup extends AbstractJob {
	
	Logger logger = Logger.getLogger(SetPermissionsOrgAdminGroup.class);

	public static final String PARAM_ORGANISATIONS = "ORGANISATIONS";
	
	ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
	
	OrganisationService organisationService = (OrganisationService)appContext.getBean("eduOrganisationService");
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		String orgs = context.getJobDetail().getJobDataMap().getString(PARAM_ORGANISATIONS);
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run((orgs != null) ? orgs.split(",") : null);
				return null;
			}
		};
		
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	private void run(String[] orgs) {
		if(orgs == null) {
			orgs = EduGroupCache.getNames();
		}
		
		logger.info("running for " + orgs.length + " orgs");
		
		int i = 0;
		for(String org : orgs) {
			logger.info("org nr" + i);
			organisationService.setOrgAdminPermissions(org);
			i++;
		}
	}
	
	
	
	@Override
	public Class[] getJobClasses() {
		//addJobClass(SetPermissionsOrgAdminGroup.class);
		
		ArrayList<Class> al = new ArrayList<Class>(Arrays.asList(allJobs));
		al.add(SetPermissionsOrgAdminGroup.class);
		return al.toArray(new Class[0]) ;
	}
	
}
