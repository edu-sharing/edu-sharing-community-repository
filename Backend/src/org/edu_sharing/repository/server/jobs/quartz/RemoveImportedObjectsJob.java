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

import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class RemoveImportedObjectsJob extends AbstractJob{
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			new PersistentHandlerEdusharing().removeAllImportedObjects();
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
