package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TrashcanCleanerSolrJob extends AbstractJob {

	public static final String PARAM_DAYS_TO_KEEP = "DAYS_TO_KEEP";
	
	public static final String PARAM_BATCH_COUNT = "BATCH_COUNT";
	
	protected static final int DEFAULT_DAYS_TO_KEEP = -1;
	protected static final int DEFAULT_DELETE_BATCH_COUNT = 1000;
	
	Logger logger = Logger.getLogger(TrashcanCleanerJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		
		final int time = jobDataMap.containsKey(PARAM_DAYS_TO_KEEP)
					? jobDataMap.getInt(PARAM_DAYS_TO_KEEP)
					: DEFAULT_DAYS_TO_KEEP;
		
		final int batch = jobDataMap.containsKey(PARAM_BATCH_COUNT)
					? jobDataMap.getInt(PARAM_BATCH_COUNT)
					: DEFAULT_DELETE_BATCH_COUNT;

		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				
				new TrashcanCleanerSolr(TimeUnit.MILLISECONDS.convert(time, TimeUnit.DAYS),batch).exeute();
				return null;
			}
		};
		AuthenticationUtil.runAs(runAs, "admin");
		//AuthenticationUtil.runAsSystem(runAs);
		
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		ArrayList<Class> list = new ArrayList(Arrays.asList(super.allJobs));
		list.add(TrashcanCleanerJob.class);
		return list.toArray(new Class[list.size()] ); 
	}
	
}
