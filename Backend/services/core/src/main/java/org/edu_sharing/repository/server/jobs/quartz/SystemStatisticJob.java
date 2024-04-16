package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.admin.SystemStatistic;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SystemStatisticJob extends AbstractJob {
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				SystemStatistic.addRepoState(AdminServiceFactory.getInstance().getCacheCluster());
				return null;
			}
		});
		
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return super.allJobs;
	}
}
