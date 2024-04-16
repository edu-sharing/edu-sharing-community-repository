package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Arrays;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.importer.MoodleImporter;
import org.edu_sharing.repository.server.importer.OPALImporter;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CourseImporterJob extends AbstractJob {

	private static final String KEY_TYPE = "type";
	private static final String KEY_SCHEME = "scheme";
	private static final String KEY_HOST = "host";
	private static final String KEY_PORT = "port";
	private static final String KEY_CONTEXT = "context";
	private static final String KEY_WSTOKEN = "wstoken";
	private static final String KEY_USER = "user";
	private static final String KEY_PASSWORD = "password";

	private static enum TYPES {
		moodle, opal
	};

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				// TODO Auto-generated method stub

				try {

					switch (TYPES.valueOf(jobDataMap.getString(KEY_TYPE))) {

					case moodle:

						new MoodleImporter(jobDataMap.getString(KEY_SCHEME), jobDataMap.getString(KEY_HOST),
								Integer.parseInt(jobDataMap.getString(KEY_PORT)), jobDataMap.getString(KEY_CONTEXT),
								jobDataMap.getString(KEY_WSTOKEN), new PersistentHandlerEdusharing(CourseImporterJob.this,null,true));
						break;

					case opal:

						new OPALImporter(jobDataMap.getString(KEY_SCHEME), jobDataMap.getString(KEY_HOST),
								Integer.parseInt(jobDataMap.getString(KEY_PORT)), jobDataMap.getString(KEY_CONTEXT),
								jobDataMap.getString(KEY_USER), jobDataMap.getString(KEY_PASSWORD),
						        new PersistentHandlerEdusharing(CourseImporterJob.this,null,true));
						break;
					}
				} catch (Throwable t) {
					throw new JobExecutionException(t);
				}

				return null;
			}
		};

		AuthenticationUtil.runAsSystem(runAs);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getJobClasses() {

		Class[] result = Arrays.copyOf(allJobs, allJobs.length + 2);
		result[result.length - 1] = MoodleImporter.class;
		result[result.length - 2] = OPALImporter.class;
		return result;
	}

}
