package org.edu_sharing.alfresco.jobs;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.log4j.Logger;
import org.apache.tika.io.TikaInputStream;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
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
	ContentService contentService = serviceRegistry.getContentService();
	NodeService nodeService = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
	MimetypeService mimetypeService = serviceRegistry.getMimetypeService();


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

				logger.debug("starting: " + context.getFireTime() + " name:"
						+ context.getJobDetail().getKey().getName());

				try {
					List<JobExecutionContext> currentlyExecutingJobs = (List<JobExecutionContext>) context
							.getScheduler().getCurrentlyExecutingJobs();
					int countPreviewJobs = 0;
					for (JobExecutionContext jec : currentlyExecutingJobs) {
						if (jec.getJobInstance().getClass().equals(PreviewJob.class)){
							countPreviewJobs++;
						}
						
						if (jec.getJobInstance().getClass().equals(PreviewJob.class)
								&& !context.getJobDetail().equals(jec.getJobDetail())) {
							logger.debug("another instance is running. returning firetime:" + jec.getFireTime());
							return null;
						}
					}
					
					logger.debug("count preview jobs:" + countPreviewJobs);
					
				} catch (SchedulerException e) {
					logger.error(e.getMessage(), e);
					return null;
				}

				logger.debug("starting, nodes with actions:" + ActionObserver.getInstance().getNodeActionsMap().size());
				ActionObserver.getInstance().removeInactiveActions();

				
				Map<NodeRef, List<Action>> m = ActionObserver.getInstance().getNodeActionsMap();
				
				/**
				 * syncronized slows process down, catching ConcurrentModificationException, that just skips one job round
				 */
				try {
				//synchronized (m) {
					runJob(m);
				//}
				}catch(ConcurrentModificationException e) {
					logger.debug("ConcurrentModificationException while runing Preview job");
				}
				
				
				logger.debug("returning");
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

			logger.debug("found  " + countRunning + " running/pending" + " countPending:" + countPending);
            int maxRunning = LightbendConfigLoader.get().getInt("repository.transformer.preview.maxRunning");;

			if (countRunning < maxRunning) {
				int newRunning = 0;
				for (Map.Entry<NodeRef, List<Action>> entry : m.entrySet()) {
					//synchronized (entry.getValue()) {
						for (Action action : entry.getValue()) {

							logger.debug("check start for id:" + action.getId() + " status "
									+ action.getExecutionStatus() + " " + action.getActionDefinitionName());
							if (action.getExecutionStatus() == ActionStatus.New
									&& action.getActionDefinitionName()
											.equals(CCConstants.ACTION_NAME_CREATE_THUMBNAIL)) {

								RunAsWork<Void> executeActionRunAs = new RunAsWork<Void>() {
									@Override
									public Void doWork() throws Exception {
										actionService.executeAction(action, entry.getKey(), true, true);
										return null;
									}
								};

								//cleanup will be done after one hour in ActionObserver.removeInactiveActions
								if(!nodeService.exists(entry.getKey())){
									return;
								}

								String creator = (String) nodeService
										.getProperty(entry.getKey(), ContentModel.PROP_CREATOR);

								boolean hasContent = false;
								ContentReader reader = contentService
										.getReader(entry.getKey(), ContentModel.PROP_CONTENT);
								if (reader != null) {
									if (reader.getSize() > 0) {
										hasContent = true;
									}
								}

								if (hasContent) {

									String name = (String) nodeService
											.getProperty(entry.getKey(), ContentModel.PROP_NAME);
									
									LockState lockState = serviceRegistry.getLockService()
											.getLockState(entry.getKey());
									logger.debug("preview job execute action for :" + name +" lock state: " + lockState.getLockType() + "  "
											+ lockState.getLifetime() + " " + lockState.getAdditionalInfo()
											+ " " + lockState);

									Date date = (Date) action
											.getParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE);

									if ((System.currentTimeMillis() > (date.getTime() + latency)) 
											) {
										if(lockState.getLockType() == null) {
											logger.debug("nodeRef: " + entry.getKey() +" runAs:" + creator);
											AuthenticationUtil.runAs(executeActionRunAs, creator);
											logger.debug("finished action syncronously. nodeRef:" + entry.getKey()
													+ " action status:" + action.getExecutionStatus()
													+ " ExecutionStartDate:" + action.getExecutionStartDate()
													+ " filename:" + name);
											newRunning++;
										}else {
											logger.debug("node " + entry.getKey() + " is locked. will try it later.");
										}
									} else {
										logger.debug(
												"will wait " + latency/1000 + " sek before starting thumnail action for" + name);
									}
								} else {
									/**
									 * @todo rember the tries and remove from ActionObserver
									 */
									logger.debug(entry.getKey() + " does not have content yet");
								}
							}
						}


					if (countRunning + newRunning >= maxRunning) {
						logger.debug("returning cause countRunning + newRunning ("+ (countRunning + newRunning)+ ") >= maxRunning "+maxRunning);
                        return;
					}
				}

			
		}
	}
}
