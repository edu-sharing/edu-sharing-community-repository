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

import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.tracking.TrackingService;
import org.edu_sharing.repository.server.tracking.collector.TrackingEventHandler;
import org.edu_sharing.repository.server.tracking.collector.TrackingEventHandler.TrackingEventHandlerContext;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This TrackingJobs is responsible to collect tracking events
 * storing in the tracking buffer and transmit this events
 * to an <code>TrackingEventHandler</code>.    
 *
 * @author thomschke
 *
 */
public class TrackingJob extends AbstractJob {

	/**
	 * job parameter: class name for <code>TrackingEventHandler</code> 
	 */
	public static final String PARAM_HANDLER_CLASS = "TrackingEventHandler";

	public TrackingJob() {
		this.logger = LogFactory.getLog(TrackingJob.class);
	}

	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		logger.info("TrackingJob started.");

		final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

		try {

			TrackingEventHandler handler = (TrackingEventHandler) Class
					.forName(jobDataMap.getString(PARAM_HANDLER_CLASS))
					.newInstance();

			logger.debug("TrackingJob: bind");
			handler.bind(new TrackingEventHandlerContext() {

				public void logInfo(String message) {
					logger.info(message);

				}

				public void logError(Throwable t) {
					logger.error(t.getMessage(), t);

				}

				public String getParameter(String parameter) {
					
					return jobDataMap.getString(parameter);
				}
			});

			logger.debug("TrackingJob: collect");
			TrackingService.collect(handler);

			logger.debug("TrackingJob: unbind");
			handler.unbind();

		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (InstantiationException e) {
			logger.error(e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("TrackingJob ended.");
	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}

}
