package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@JobDescription(description = "job to cleanup archive")
public class TrashcanCleanerSolrJob extends AbstractJobMapAnnotationParams {

	@JobFieldDescription(description = "nr of days to keep")
	public Integer DAYS_TO_KEEP;

	@JobFieldDescription(description = "batch count")
	public Integer BATCH_COUNT;

	@JobFieldDescription(description = "if false run in protocol mode")
	Boolean execute = true;
	
	protected static final int DEFAULT_DAYS_TO_KEEP = -1;
	protected static final int DEFAULT_DELETE_BATCH_COUNT = 1000;
	
	Logger logger = Logger.getLogger(TrashcanCleanerJob.class);
	
	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		final int time = (DAYS_TO_KEEP != null)
					? DAYS_TO_KEEP
					: DEFAULT_DAYS_TO_KEEP;
		
		final int batch = (BATCH_COUNT != null)
					? BATCH_COUNT
					: DEFAULT_DELETE_BATCH_COUNT;

		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				
				new TrashcanCleanerSolr(TimeUnit.MILLISECONDS.convert(time, TimeUnit.DAYS),batch, execute).exeute();
				return null;
			}
		};
		AuthenticationUtil.runAs(runAs, "admin");
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		ArrayList<Class> list = new ArrayList(Arrays.asList(super.allJobs));
		list.add(TrashcanCleanerJob.class);
		return list.toArray(new Class[list.size()] ); 
	}
	
}
