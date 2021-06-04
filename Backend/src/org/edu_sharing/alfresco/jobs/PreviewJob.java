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

import com.googlecode.mp4parser.FileDataSourceImpl;
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
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;

public class PreviewJob implements Job {

	ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext()
			.getBean(ServiceRegistry.SERVICE_REGISTRY);
	ActionService actionService = serviceRegistry.getActionService();
	ContentService contentService = serviceRegistry.getContentService();
	NodeService nodeService = serviceRegistry.getNodeService();
	MimetypeService mimetypeService = serviceRegistry.getMimetypeService();

	int maxRunning = 5;

	Logger logger = Logger.getLogger(PreviewJob.class);
	
	/**
	 * 5 seconds latency before starting
	 */
	long latency = 5000;
	
	
	private void extractVideoImageMetadata(NodeRef nodeRef, String runAs) {
		RunAsWork<Void> videoImageMetadataExtractor = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

				try{
					String notMatchesMimetype = mimetypeService.getMimetypeIfNotMatches(reader);
					if(notMatchesMimetype != null){
						logger.error("mimetype does not match, maybe file was renamed " + nodeRef +" guessed: "+ reader.getMimetype() +" heuristic: " + notMatchesMimetype);
						return null;
					}
				}
				catch (ContentIOException cioe)
				{
					logger.error(cioe);
					return null;
				}

				// alfresco does not read image size for all images, so we try to fix it
				// trying to load not the whole image but just the bounding rect, see also:
				// http://stackoverflow.com/questions/1559253/java-imageio-getting-image-dimensions-without-reading-the-entire-file
				if(reader.getMimetype().contains("image")){
					InputStream is = null;
					try{
						is = reader.getContentInputStream();
						try(ImageInputStream in = ImageIO.createImageInputStream(is)){
						    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
						    if (readers.hasNext()) {
						        ImageReader imageReader = readers.next();
						        try {
						        	imageReader.setInput(in);
						        	nodeService.setProperty(nodeRef, QName.createQName(CCConstants.EXIF_PROP_PIXELXDIMENSION), imageReader.getWidth(0));
									nodeService.setProperty(nodeRef, QName.createQName(CCConstants.EXIF_PROP_PIXELYDIMENSION), imageReader.getHeight(0));
						        } finally {
						        	imageReader.dispose();
						        }
						    }
						} 
					}catch(Throwable t){
					} finally {
						if(is != null){
							is.close();
						}
					}
				}
				return null;
			}
		};
		
		AuthenticationUtil.runAs(videoImageMetadataExtractor, runAs);
	}


	

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
					int countPreviewJobs = 0;
					for (JobExecutionContext jec : currentlyExecutingJobs) {
						if (jec.getJobInstance().getClass().equals(PreviewJob.class)){
							countPreviewJobs++;
						}
						
						if (jec.getJobInstance().getClass().equals(PreviewJob.class)
								&& !context.getJobDetail().equals(jec.getJobDetail())) {
							logger.info("another instance is running. returning firetime:" + jec.getFireTime());
							return null;
						}
					}
					
					logger.info("count preview jobs:" + countPreviewJobs);
					
				} catch (SchedulerException e) {
					logger.error(e.getMessage(), e);
					return null;
				}

				logger.info("starting, nodes with actions:" + ActionObserver.getInstance().getNodeActionsMap().size());
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
										actionService.executeAction(action, entry.getKey(), true, true);
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

									if ((System.currentTimeMillis() > (date.getTime() + latency)) 
											) {
										if(lockState.getLockType() == null) {
											logger.debug("nodeRef:" + entry.getKey() +" runAs:" + creator);
											extractVideoImageMetadata(entry.getKey(),creator);
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
