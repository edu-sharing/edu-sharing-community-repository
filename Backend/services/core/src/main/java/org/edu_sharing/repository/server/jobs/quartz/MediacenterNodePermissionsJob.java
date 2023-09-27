package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.text.ParseException;
import java.util.Date;

@JobDescription(description = "Sync permissions for imported objects based on a custom implementation of a MediacenterLicenseProvider")
public class MediacenterNodePermissionsJob extends AbstractInterruptableJob implements JobClusterLocker.ClusterSingelton{

	@JobFieldDescription(description = "Period in days to look back for license changes, empty value for full refresh")
	Integer period_in_days;

	Logger logger = Logger.getLogger(MediacenterNodePermissionsJob.class);
	
	@Override
	public void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

		Date fromLocal = null;
		Date untilLocal = null;
		try {
			fromLocal = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_FROM));
			untilLocal = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_UNTIL));
		} catch (ParseException|NullPointerException e) {
			logger.info(OAIConst.PARAM_FROM + " and " + OAIConst.PARAM_UNTIL + " was not set or could not be parsed and will be ignored: " + e.getMessage());
		}

		if(fromLocal == null && untilLocal == null){
			if(period_in_days != null) {
				Long periodInDays = new Long(period_in_days);
				Long periodInMs = periodInDays * 24 * 60 * 60 * 1000;
				untilLocal = new Date();
				fromLocal = new Date((untilLocal.getTime() - periodInMs));
				logger.info("using from:" + fromLocal + " until:" + untilLocal);
			}
		}

		Date from = fromLocal;
		Date until = untilLocal;
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			
			@Override
			public Void doWork() throws Exception {
				run(from,until);
				return null;
			}
		};
		
		AuthenticationUtil.runAsSystem(runAs);
		
	}
	
	private void run(Date from, Date until) {
		MediacenterServiceFactory.getLocalService().manageNodeLicenses(from, until);
	}
	

	@Override
	public Class[] getJobClasses() {
		this.addJobClass(MediacenterNodePermissionsJob.class);
		return this.allJobs;
	}

}
