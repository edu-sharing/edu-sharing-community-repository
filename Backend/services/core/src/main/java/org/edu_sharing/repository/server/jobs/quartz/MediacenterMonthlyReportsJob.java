package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.statistic.StatisticService;
import org.edu_sharing.service.statistic.StatisticServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@JobDescription(description = "Creates reports for all mediacenters on the 1st of each month for the last month")
public class MediacenterMonthlyReportsJob extends AbstractJobMapAnnotationParams {

	protected Logger logger = Logger.getLogger(MediacenterMonthlyReportsJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private NodeService nodeServiceEdu;

	@JobFieldDescription(description = "force run, even if the date is currently not the 1st")
	private boolean force;


	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if(!force && localDate.getDayOfMonth() != 1) {
			logger.info("Job not running because of date: " + localDate.getDayOfMonth());
		}
		try {
			StatisticService statisticsService = StatisticServiceFactory.getLocalService();
			MediacenterService mediacenterService = MediacenterServiceFactory.getLocalService();
			for (String mediacenter : SearchServiceFactory.getLocalService().getAllMediacenters()) {

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
