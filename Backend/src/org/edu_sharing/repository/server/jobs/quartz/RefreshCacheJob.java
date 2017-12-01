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

import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class RefreshCacheJob extends AbstractJob{
	
	
	public RefreshCacheJob(){
		this.logger = LogFactory.getLog(RefreshCacheJob.class);
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.info("starting");
		
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		
		String rootFolderId = (String)jobDataMap.get("rootFolderId");
		boolean sticky = new Boolean((String)jobDataMap.get("sticky"));
		
		HashMap authInfo = (HashMap)jobDataMap.get("authInfo");
		
		try {
			new RefreshCacheExecuter().excecute(rootFolderId, sticky, authInfo);
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
