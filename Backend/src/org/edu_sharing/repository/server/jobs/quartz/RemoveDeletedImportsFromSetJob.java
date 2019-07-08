package org.edu_sharing.repository.server.jobs.quartz;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.ImportCleanerIdentifiersList;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RemoveDeletedImportsFromSetJob extends AbstractJob {

	
	public static final String PARAM_URL = "URL";
	
	public static final String PARAM_SET = "SET";
	
	public static final String PARAM_METADATAPREFIX = "METADATA_PREFIX";
	
	public static final String PARAM_TESTMODE = "TESTMODE";
	
	Logger logger = Logger.getLogger(RemoveDeletedImportsFromSetJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		String url = (String)context.getJobDetail().getJobDataMap().get(PARAM_URL);
		String set = (String)context.getJobDetail().getJobDataMap().get(PARAM_SET);
		String metadataPrefix = (String)context.getJobDetail().getJobDataMap().get(PARAM_METADATAPREFIX);
		String testModeP = (String)context.getJobDetail().getJobDataMap().get(PARAM_TESTMODE);
		
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				if(url == null) {
					logger.error("missing param " + PARAM_URL);
					return null;
				}
				
				if(set == null) {
					logger.error("missing param " + PARAM_SET);
					return null;
				}
				
				if(metadataPrefix == null) {
					logger.error("missing param " + PARAM_METADATAPREFIX);
					return null;
				}
				
				Boolean testMode = (testModeP == null) ? new Boolean("true") : new Boolean(testModeP);
				new ImportCleanerIdentifiersList(url, set, metadataPrefix, testMode);
				return null;
			}
		};
		
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	@Override
	public Class[] getJobClasses() {
		super.addJobClass(RemoveDeletedImportsFromSetJob.class);
		return super.allJobs;
	}
}
