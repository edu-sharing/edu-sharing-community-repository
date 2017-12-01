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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;

public abstract class AbstractJob implements Job {
	
	protected Log logger = LogFactory.getLog(AbstractJob.class);
	
	boolean isStarted = false;
	
	protected Class[] allJobs =  new Class[] { ImporterJob.class, RefreshCacheJob.class,RemoveDeletedImportsJob.class,RemoveImportedObjectsJob.class,GetAllDamagedObjects.class,RefreshPublisherListJob.class, TrackingJob.class, ExporterJob.class,RefreshValuespaceFileJob.class};
	
	//important for immediate executed Jobs so that we can give an user feedback if the job was vetoed
	//boolean vetoed = false;
	
	public boolean isStarted() {
		return isStarted;
	}
	
	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}


	public abstract Class[] getJobClasses();
	
	
	
	
}
