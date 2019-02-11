package org.edu_sharing.alfresco.jobs;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public class PreviewJob implements Job {

	ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext()
			.getBean(ServiceRegistry.SERVICE_REGISTRY);
	ActionService actionService = serviceRegistry.getActionService();

	int maxRunning = 5;

	Logger logger = Logger.getLogger(PreviewJob.class);
	
	/**
	 * 5 seconds latency before starting
	 */
	long latency = 5000;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		RunAsWork<Void> runAsP = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {

				logger.info("PreviewJob is starting firetime:" + context.getFireTime() + " name:"
						+ context.getJobDetail().getName());

				try {
					List<JobExecutionContext> currentlyExecutingJobs = (List<JobExecutionContext>) context
							.getScheduler().getCurrentlyExecutingJobs();
					for (JobExecutionContext jec : currentlyExecutingJobs) {
						if (jec.getJobInstance().getClass().equals(PreviewJob.class)
								&& !context.getJobDetail().equals(jec.getJobDetail())) {
							logger.info("another instance is running. returning firetime:" + jec.getFireTime());
							return null;
						}
					}
				} catch (SchedulerException e) {
					logger.error(e.getMessage(), e);
					return null;
				}

				logger.info("starting, nodes with actions:" + ActionObserver.getInstance().getNodeActionsMap().size());
				ActionObserver.getInstance().removeInactiveActions();

				
				Map<NodeRef, List<Action>> m = ActionObserver.getInstance().getNodeActionsMap();
				
				synchronized (m) {
					runJob(m);
				}
				
				
				logger.info("returning");
				return null;
			}
		};

		AuthenticationUtil.runAsSystem(runAsP);
	}
	

	
	public void runJob(Map<NodeRef, List<Action>> m) {
		int countRunning = 0;
		int countPending = 0;
		
		
			for (Map.Entry<NodeRef, List<Action>> entry : m.entrySet()) {
				for (Action action : entry.getValue()) {
					logger.debug("action status:" + action.getExecutionStatus() + " created Date:"
							+ action.getParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE));
					if (action.getExecutionStatus() == ActionStatus.Running
							|| action.getExecutionStatus() == ActionStatus.Pending) {
						countRunning++;
					}
					
					if (action.getExecutionStatus() == ActionStatus.Pending) {
						countPending++;
					}
				}
			}

			logger.info("found " + countRunning + " running/pending" + " countPending:" + countPending);

			if (countRunning < maxRunning) {
				int newRunning = 0;
				for (Map.Entry<NodeRef, List<Action>> entry : m.entrySet()) {
					synchronized (entry.getValue()) {
						for (Action action : entry.getValue()) {

							logger.debug("check start for id:" + action.getId() + " status "
									+ action.getExecutionStatus() + " " + action.getActionDefinitionName());
							if (action.getExecutionStatus() == ActionStatus.New
									&& action.getActionDefinitionName()
											.equals(CCConstants.ACTION_NAME_CREATE_THUMBNAIL)) {

								RunAsWork<Void> executeActionRunAs = new RunAsWork<Void>() {
									@Override
									public Void doWork() throws Exception {
										actionService.executeAction(action, entry.getKey(), true, false);
										return null;
									}
								};

								String creator = (String) serviceRegistry.getNodeService()
										.getProperty(entry.getKey(), ContentModel.PROP_CREATOR);

								boolean hasContent = false;
								ContentReader reader = serviceRegistry.getContentService()
										.getReader(entry.getKey(), ContentModel.PROP_CONTENT);
								if (reader != null) {
									if (reader.getSize() > 0) {
										hasContent = true;
									}
								}

								if (hasContent) {

									String name = (String) serviceRegistry.getNodeService()
											.getProperty(entry.getKey(), ContentModel.PROP_NAME);
									
									LockState lockState = serviceRegistry.getLockService()
											.getLockState(entry.getKey());
									logger.debug("preview job execute action for :" + name +" lock state: " + lockState.getLockType() + "  "
											+ lockState.getLifetime() + " " + lockState.getAdditionalInfo()
											+ " " + lockState);

									Date date = (Date) action
											.getParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE);

									if (System.currentTimeMillis() > (date.getTime() + latency)) {
										AuthenticationUtil.runAs(executeActionRunAs, creator);
										logger.debug("finished action syncronously. nodeRef:" + entry.getKey()
												+ " action status:" + action.getExecutionStatus()
												+ " ExecutionStartDate:" + action.getExecutionStartDate()
												+ " filename:" + name);
										newRunning++;
									} else {
										logger.debug(
												"will wait " + latency/1000 + " sek before starting thumnail action for" + name);
									}
								} else {
									/**
									 * @todo rember the tries and remove from ActionObserver
									 */
									logger.info(entry.getKey() + " does not have content yet");
								}
							}
						}
					}

					if (countRunning + newRunning >= maxRunning) {
						logger.info("returning cause countRunning + newRunning >= maxRunning");
					}
				}

			
		}
	}
}
