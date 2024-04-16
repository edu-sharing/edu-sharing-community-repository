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
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobDescription(description = "Remove all imported nodes")
public class RemoveImportedObjectsJob extends AbstractJob{
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		
		String username = (String) context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_USERNAME);
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run();
				return null;
			}
		};
		
		AuthenticationUtil.runAs(runAs,username);
	
	}
	
	public void run() {
		try {
			PersistentHandlerEdusharing.removeAllImportedObjects(this);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
