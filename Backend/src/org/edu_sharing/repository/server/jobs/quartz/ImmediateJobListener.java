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

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobListener;


/**
 * merkt sich den status für userinformationen, entfernt sich und den job
 * @author rudi
 *
 */
public class ImmediateJobListener implements JobListener {
	
	String jobName = null;
	
	boolean vetoed = false;
	
	boolean wasExecuted = false;
	
	String vetoBy = null;
	
	public ImmediateJobListener(String name){
		this.jobName = name;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.jobName;
	}
	
	@Override
	public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {
		vetoed = true; 
		try{
			System.out.println("ImmediateJobListener VETOED!");
			vetoBy = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(JobHandler.VETO_BY_KEY);
			Logger.getLogger(jobExecutionContext.getJobDetail().getJobClass()).error("Job was vetoed by "+vetoBy);
            JobHandler.getInstance().finishJob(jobExecutionContext.getJobDetail(),JobInfo.Status.Aborted);
            jobExecutionContext.getScheduler().deleteJob(this.jobName, null);
			jobExecutionContext.getScheduler().removeJobListener(this.jobName);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void jobToBeExecuted(JobExecutionContext arg0) {
		
	};
	
	public void jobWasExecuted(JobExecutionContext jobExecutionContext, org.quartz.JobExecutionException arg1) {
		wasExecuted = true;
		try{
			jobExecutionContext.getScheduler().deleteJob(this.jobName, null);
			jobExecutionContext.getScheduler().removeJobListener(this.jobName);
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
