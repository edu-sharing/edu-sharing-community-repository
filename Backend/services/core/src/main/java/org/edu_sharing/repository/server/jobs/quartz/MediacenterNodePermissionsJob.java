package org.edu_sharing.repository.server.jobs.quartz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.HttpException;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class MediacenterNodePermissionsJob extends AbstractJob {
	
	Logger logger = Logger.getLogger(MediacenterNodePermissionsJob.class);
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

		Date fromLocal = null;
		Date untilLocal = null;
		try {
			fromLocal = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_FROM));
			untilLocal = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_UNTIL));
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}catch (NullPointerException e){
			logger.error(e.getMessage());
		}

		if(fromLocal == null && untilLocal == null){
			String periodInDaysStr = (String)jobDataMap.get(OAIConst.PARAM_PERIOD_IN_DAYS);
			if(periodInDaysStr != null && !periodInDaysStr.trim().equals("")) {
				Long periodInDays = new Long(periodInDaysStr);
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
