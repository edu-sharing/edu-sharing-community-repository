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

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.service.nodeservice.NodeFrontpage;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class UpdateFrontpageCacheJob extends AbstractJob{


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.runAsSystem(() -> {
			new NodeFrontpage().buildCache(this);
			return null;
		});
	
	}

	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
