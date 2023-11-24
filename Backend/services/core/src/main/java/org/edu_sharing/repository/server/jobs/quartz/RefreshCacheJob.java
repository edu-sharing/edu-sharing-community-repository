/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import java.util.HashMap;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


@JobDescription(description = "Re-Build/warmup cache for the IMP-OBJ or a custom folder")
public class RefreshCacheJob extends AbstractInterruptableJob implements JobClusterLocker.ClusterSingelton {


	@JobFieldDescription(description = "the node id to start from (defaults to IMP-OBJ)")
	String rootFolderId;
	
	public RefreshCacheJob(){
		this.logger = LogFactory.getLog(RefreshCacheJob.class);
	}
	
	@Override
	public void executeInterruptable(JobExecutionContext context) throws JobExecutionException {
		logger.info("starting");

		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		
		final boolean sticky;
		if(jobDataMap.get("sticky") != null) {
			if (jobDataMap.get("sticky") instanceof Boolean) {
				sticky = (boolean) jobDataMap.get("sticky");
			} else {
				sticky = new Boolean((String) jobDataMap.get("sticky"));
			}
		} else {
			sticky = true;
		}
		
		HashMap authInfo = (HashMap)jobDataMap.get("authInfo");
		
		try {
			if(authInfo == null) {
				RunAsWork<Void> runAs = new RunAsWork<Void>() {
					@Override
					public Void doWork() throws Exception {
						try {
							new RefreshCacheExecuter().excecute(rootFolderId, sticky, authInfo);
						} catch (Throwable e) {
							logger.error(e);
						}
						return null;
					}
				};
				AuthenticationUtil.runAsSystem(runAs);
			}else {
				new RefreshCacheExecuter().excecute(rootFolderId, sticky, authInfo);
			}
		} catch (Throwable e) {
			logger.error("I will throw an JobExecutionException:"+e.getMessage());
			throw new JobExecutionException(e);
		}
		
		logger.info("returns");
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}

}
