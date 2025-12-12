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

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.JobListener;


/**
 * merkt sich den status für userinformationen, entfernt sich und den job
 * @author rudi
 *
 */
@RequiredArgsConstructor
public class ImmediateJobListener implements JobListener {
	final JobDetail jobDetail;

	boolean vetoed = false;
	
	boolean wasExecuted = false;
	
	String vetoBy = null;

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.jobDetail.getKey().getName();
	}
	
	@Override
	public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {
		vetoed = true; 
		try{
			System.out.println("ImmediateJobListener VETOED!");
			vetoBy = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(JobHandler.VETO_BY_KEY);
			Logger.getLogger(jobDetail.getJobClass()).error("Job was vetoed by "+vetoBy);
            JobHandler.getInstance().finishJob(jobDetail,JobInfo.Status.Aborted);
			jobExecutionContext.getScheduler().deleteJob(jobDetail.getKey());
			jobExecutionContext.getScheduler().getListenerManager().removeJobListener(this.jobDetail.getKey().getName());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void jobToBeExecuted(JobExecutionContext arg0) {
		
	};
	
	public void jobWasExecuted(JobExecutionContext jobExecutionContext, org.quartz.JobExecutionException arg1) {
		wasExecuted = true;
		try{
			jobExecutionContext.getScheduler().deleteJob(JobKey.jobKey(this.jobDetail.getKey().getName()));
			jobExecutionContext.getScheduler().getListenerManager().removeJobListener(this.jobDetail.getKey().getName());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public boolean isVetoed() {
		return vetoed;
	}

	public void setVetoed(boolean vetored) {
		this.vetoed = vetored;
	}
	

	public String getVetoBy() {
		return vetoBy;
	}

	public boolean wasExecuted() {
		return wasExecuted;
	}	
	
	
	
}
