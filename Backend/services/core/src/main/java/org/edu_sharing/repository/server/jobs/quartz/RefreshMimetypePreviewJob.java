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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@JobDescription(description = "refresh the mimetype and preview for a given solr search filter")
public class RefreshMimetypePreviewJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(RefreshMimetypePreviewJob.class);
	@JobFieldDescription(description = "define a filter for files that will be refreshed for mimetype and preview", sampleValue = "@cclom\\:format:\"text/xml*\"")
	private String filter;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.runAsSystem(() -> {
			doWork(context);
			return null;
		});
	}

	private void doWork(JobExecutionContext context) {
		NodeService nodeService = serviceRegistry.getNodeService();
		SearchService searchService = serviceRegistry.getSearchService();
		ContentService contentService = serviceRegistry.getContentService();
		MimetypeService mimetypeService = serviceRegistry.getMimetypeService();
		LockService lockService = serviceRegistry.getLockService();

		try {
			filter = context.getJobDetail().getJobDataMap().getString("filter");
			logger.info("using filter:" + filter);

			if (filter == null || filter.trim().equals("")) {
				logger.error("no filter defined. will stop processing!");
				return;
			}

			SearchParameters sp = new SearchParameters();
			sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
			sp.setQuery(filter);
			sp.setLanguage(SearchService.LANGUAGE_LUCENE);
			ResultSet resultSet = searchService.query(sp);
			logger.info("found:" + resultSet.length());
			for (final NodeRef nodeRef : resultSet.getNodeRefs()) {
				QName typeQName = nodeService.getType(nodeRef);
				String type = typeQName.getLocalName();
				String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);

				logger.info("name:" + name + " type:" + type);

				if (typeQName.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {

					if(nodeService.getAspects(nodeRef).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
						logger.warn("ignoring collection_io_reference:" + nodeRef);
						continue;
					}

					ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
					if(contentReader == null || !contentReader.exists()) {
						logger.warn("no content found:" + nodeRef);
						continue;
					}

					String oldMimeType = contentReader.getMimetype();

					//set to null so that guessMimetype() will not use old one
					contentReader.setEncoding(null);
					contentReader.setMimetype(null);

					String newMimetype = mimetypeService.guessMimetype(name, contentReader);
					logger.info("oldMimeType:" + oldMimeType + " newMimeType:" + newMimetype);
					try {

						lockService.lock(nodeRef, LockType.WRITE_LOCK);

						ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);

						//call preview action cause call in policy only works for new content
						writer.addListener(new ContentStreamListener() {
							@Override
							public void contentStreamClosed() throws ContentIOException {
								logger.info("finished setting new mimetype");
								ActionService actionService = serviceRegistry.getActionService();
								Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
								thumbnailAction.setTrackStatus(true);

								thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
								ActionObserver.getInstance().addAction(nodeRef, thumbnailAction);

								//cause its already async set executeAsynchronously to false
								actionService.executeAction(thumbnailAction, nodeRef, true, false);
								logger.info("finished setting new preview");
							}
						});
						writer.setEncoding("UTF-8");
						writer.setMimetype(newMimetype);
						writer.putContent(contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getContentInputStream());
						nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), newMimetype);

					} finally {
						lockService.unlock(nodeRef);
					}
				}

			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
