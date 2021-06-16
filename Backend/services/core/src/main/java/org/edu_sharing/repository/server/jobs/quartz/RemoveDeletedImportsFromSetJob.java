package org.edu_sharing.repository.server.jobs.quartz;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.ImportCleanerIdentifiersList;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author mv
 *
 * example:
 *	{"URL":"http://sodis.de/cp/oai_pmh/oai.php","SET":"oer_mebis_activated","METADATA_PREFIX":"oai_lom-de","TESTMODE":"true"}
 *
 */
public class RemoveDeletedImportsFromSetJob extends AbstractJob {

	public static final String PARAM_TESTMODE = "TESTMODE";

	Logger logger = Logger.getLogger(RemoveDeletedImportsFromSetJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		String url = (String)context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_OAI_BASE_URL);
		String setsParam = (String)context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_OAI_SETS);
		String metadataPrefix = (String)context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_OAI_METADATA_PREFIX);
		String testModeP = (String)context.getJobDetail().getJobDataMap().get(PARAM_TESTMODE);
		
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				if(url == null) {
					logger.error("missing param " + OAIConst.PARAM_OAI_BASE_URL);
					return null;
				}
				
				if(setsParam == null) {
					logger.error("missing param " + OAIConst.PARAM_OAI_SETS);
					return null;
				}
				
				if(metadataPrefix == null) {
					logger.error("missing param " + OAIConst.PARAM_OAI_METADATA_PREFIX);
					return null;
				}
				
				Boolean testMode = (testModeP == null) ? true : Boolean.valueOf(testModeP);
				String[] sets = setsParam.split(",");
				for(String set : sets){
					new ImportCleanerIdentifiersList(url, set.trim(), metadataPrefix, testMode);
				}
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
