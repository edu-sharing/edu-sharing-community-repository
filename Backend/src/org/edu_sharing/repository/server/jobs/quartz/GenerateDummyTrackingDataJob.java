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

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@JobDescription(description = "Job to create random tracking data from all nodes (only for tests!)")
public class GenerateDummyTrackingDataJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(GenerateDummyTrackingDataJob.class);
	@JobFieldDescription(description = "count of tracking data to generate (per node)")
	private Integer count;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getJobDetail().getJobDataMap();
		if(!map.containsKey("count")) {
			throw new IllegalArgumentException("Missing required field 'count'");
		}
		count = map.getInt("count");
		try {
			SearchResult<String> users = AuthenticationUtil.runAsSystem(() ->
					SearchServiceFactory.getLocalService().findAuthorities(
							AuthorityType.USER,
							"",
							true,
							0,
							count,
							null,
							null
					)
			);
			List<TrackingService.EventType> EVENT =
					Collections.unmodifiableList(Arrays.asList(TrackingService.EventType.values()));

			AtomicInteger totalCount = new AtomicInteger();
			NodeRunner runner = new NodeRunner();
			TrackingService trackingService = TrackingServiceFactory.getTrackingService();
			runner.setTask((ref) -> {
				users.getData().forEach((u) -> {
					NodeTrackingDetails details = new NodeTrackingDetails(ref.getId(), null);
					trackingService.trackActivityOnNode(
							ref,
							details,
							EVENT.get((int) (Math.random() * EVENT.size()))
					);
					totalCount.getAndIncrement();
				});
			});
			runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
			runner.setRunAsSystem(true);
			runner.setThreaded(false);
			runner.setTransaction(NodeRunner.TransactionMode.None);
			int count=runner.run();
			logger.info("Created a total of " + totalCount.get() +" events");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}


	}
}
