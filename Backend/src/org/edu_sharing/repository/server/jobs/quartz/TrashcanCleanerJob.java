package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;

@JobDescription(description = "Cleanup trashcan ")
public class TrashcanCleanerJob extends AbstractJob {

	protected static final int DEFAULT_DAYS_TO_KEEP = -1;
	protected static final int DEFAULT_DELETE_BATCH_COUNT = 1000;

	@JobFieldDescription(description = "Days which should NOT be deleted, e.g. 30  -> keep the last 30 days (-1 -> Cleaning trashcan will be skipped )", sampleValue = "-1")
	public int DAYS_TO_KEEP;
	@JobFieldDescription(description = "How much nodes per run, e.g. 1000 ", sampleValue = "1000")
	public int BATCH_COUNT;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		
		try {
			final int time =   jobDataMap.containsKey("DAYS_TO_KEEP")
					? jobDataMap.getInt("DAYS_TO_KEEP")
					: DEFAULT_DAYS_TO_KEEP;

			int batch =   jobDataMap.containsKey("BATCH_COUNT")
					? jobDataMap.getInt("BATCH_COUNT")
					: DEFAULT_DELETE_BATCH_COUNT;

			new TrashcanCleaner(TimeUnit.MILLISECONDS.convert(time, TimeUnit.DAYS),batch).execute();
		} catch (Throwable t) {
			throw new JobExecutionException(t);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getJobClasses() {
		
		Class[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = TrashcanCleanerJob.class;
		return result;
	}
	
	public static class TrashcanCleaner {

		private final long timeToKeep;
		private final int batchSize;
		
		private final Logger logger = Logger.getLogger(TrashcanCleaner.class);

		private final String ASSOC_CHILDREN = "{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}children";
		private final String PROP_ARCHIVED_DATE = "{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}archivedDate";
		
		TrashcanCleaner (long timeToKeep, int batchSize) {
			this.timeToKeep = timeToKeep;
			this.batchSize = batchSize;
		}

		void execute() throws Throwable {

			if (timeToKeep < 0) {
				logger.info("Cleaning trashcan will be skipped (timeToKeep undefined).");
				return;
			}

			ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			HashMap<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());

			MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(null, authInfo);
			
			StoreRef storeRef = new StoreRef("archive://SpacesStore");
			
			logger.info("Cleaning trashcan starting (timeToKeep: " + timeToKeep + ", batchSize: " + batchSize +")");

			HashMap<String, HashMap<String, Object>> childrenByAssociation = 
					apiClient.getChildrenByAssociation(
						storeRef, 
						apiClient.getRootNode(storeRef), 
						ASSOC_CHILDREN);
			
			int i = 0;
			if (childrenByAssociation != null) {

				logger.info("Cleaning trashcan scanning (size: " + childrenByAssociation.size() +")");

				long now = System.currentTimeMillis();
				
				for (Entry<String, HashMap<String, Object>> entry : childrenByAssociation.entrySet()) {
					
					String archivedDate = (String) entry.getValue().get(PROP_ARCHIVED_DATE);
					
					if (archivedDate == null) {
						
						continue;
					}					

					long lifeTime = now - Long.parseLong(archivedDate);
					
					if (lifeTime <= timeToKeep) {
						
						logger.debug("Cleaning trashcan will be skipped (node: " + entry.getKey() + ", lifeTime: " + lifeTime +")");
						continue;
					}

					if (++i > batchSize) {

						logger.info("Cleaning trashcan will be skipped (batchSize reached)");
						break;
					}

					apiClient.removeNode(storeRef, entry.getKey());
					logger.info("Cleaning trashcan performed (node: " + entry.getKey() + ")");
				}				
			}
			
			logger.info("Cleaning trashcan finished (size: " + i + ")");
			
		}
		
	}

}
