package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Arrays;

import org.edu_sharing.repository.server.importer.ValuespaceImporter;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ValuespaceImporterJob extends AbstractJob {

	private static final String KEY_LOCALE = "locale";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

		try {
			
			new ValuespaceImporter(jobDataMap.getString(KEY_LOCALE)).run();
			
		} catch (Throwable t) {
			throw new JobExecutionException(t);
		}
				
	}

	@Override
	public Class[] getJobClasses() {
		
		Class[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = ValuespaceImporterJob.class;
		return result;
	}

}
