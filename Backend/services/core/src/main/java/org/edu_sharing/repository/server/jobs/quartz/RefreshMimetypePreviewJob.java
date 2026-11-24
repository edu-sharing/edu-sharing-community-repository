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

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import jakarta.transaction.UserTransaction;
import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.repo.version.common.VersionUtil;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JobDescription(description = "refresh the mimetype and preview for a given elastic search filter and optionally the mimetype of the version history")
public class RefreshMimetypePreviewJob extends AbstractJobMapAnnotationParams {
	protected Logger logger = Logger.getLogger(RefreshMimetypePreviewJob.class);
	@JobFieldDescription(description = "define a filter for files that will be refreshed for mimetype and preview", sampleValue = "{\"term\": {\"properties.cclom:format\":\"text/xml\"}}")
	private String filter;

	@JobFieldDescription(description = "if true only thumbail generation will be don's. mimetype fix will be skipped. default is false.")
	Boolean skipMimeTypeFix;

	@JobFieldDescription(description = "thumbnailservice checks if thumbnail already exists and skips process when exists. when cleanUpExistingThumbnail is true the thumbnail node with name imagepreview will be deleted. default is false.")
	Boolean cleanUpExistingThumbnail;

	@JobFieldDescription(description = "if true the mimetype of all versions in the version history will be detected again as well (metadata only, no content rewrite, no preview generation). default is false.")
	Boolean fixVersionHistory;

	@JobFieldDescription(description = "if true the mimetype (of the node and of its versions) will only be refreshed when it is invalid (empty or without '/', e.g. 'auto'). Valid nodes are skipped completely, including preview generation. default is false.")
	Boolean onlyInvalidMimetype;

	@JobFieldDescription(description = "if false job runs in protocol mode: detected mimetypes are only logged, nothing is written and no preview is generated. default is false.")
	Boolean execute;

	@JobFieldDescription(description = "if true the filter is applied to the archive store (trash) instead of the workspace. Archived nodes only get their mimetype metadata fixed (no content rewrite, no preview generation). default is false.")
	Boolean archive;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	private BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");
	NodeService nodeService = serviceRegistry.getNodeService();
	SearchService searchService = serviceRegistry.getSearchService();
	ContentService contentService = serviceRegistry.getContentService();
	MimetypeService mimetypeService = serviceRegistry.getMimetypeService();
	VersionService versionService = serviceRegistry.getVersionService();


	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.runAsSystem(() -> {
			doWork(context);
			return null;
		});
	}

	private void doWork(JobExecutionContext context) {


		try {
			logger.info("using filter:" + filter);

			if (filter == null || filter.trim().equals("")) {
				logger.error("no filter defined. will stop processing!");
				return;
			}

			if(skipMimeTypeFix == null) skipMimeTypeFix = false;
			logger.info("using skipMimeTypeFix:" + skipMimeTypeFix);

			if(cleanUpExistingThumbnail == null) cleanUpExistingThumbnail = false;
			logger.info("using cleanUpExistingThumbnail:" + cleanUpExistingThumbnail);

			if(fixVersionHistory == null) fixVersionHistory = false;
			logger.info("using fixVersionHistory:" + fixVersionHistory);

			if(onlyInvalidMimetype == null) onlyInvalidMimetype = false;
			logger.info("using onlyInvalidMimetype:" + onlyInvalidMimetype);

			if(execute == null) execute = false;
			logger.info("using execute:" + execute + (execute ? "" : " (protocol mode, nothing will be changed)"));

			if(archive == null) archive = false;
			logger.info("using archive:" + archive);


			SearchToken searchToken = new SearchToken();
			searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(filter.getBytes()))).build());
			searchToken.setFrom(0);
			searchToken.setMaxResult(Integer.MAX_VALUE);
			if (archive) {
				searchToken.setStoreProtocol(MCAlfrescoAPIClient.archiveStoreRef.getProtocol());
				searchToken.setStoreName(MCAlfrescoAPIClient.archiveStoreRef.getIdentifier());
			}
			org.edu_sharing.service.search.SearchService localService = SearchServiceFactory.getLocalService();
			SearchResultNodeRef search = localService.search(searchToken);
			logger.info("found:" + search.getNodeCount());
			search.getData().forEach(n -> {
				NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId());
				QName typeQName = nodeService.getType(nodeRef);
				String type = typeQName.getLocalName();
				String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);

				logger.info("name:" + name + " type:" + type);

				if (typeQName.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {

					if(nodeService.getAspects(nodeRef).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
						logger.warn("ignoring collection_io_reference:" + nodeRef);
						return;
					}

					UserTransaction nonPropagatingUserTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
					try {
						nonPropagatingUserTransaction.begin();
						try {
							policyBehaviourFilter.disableBehaviour(nodeRef);
							if (onlyInvalidMimetype && isValidMimetype(getMimetype(nodeRef))) {
								logger.info("skipping node with valid mimetype:" + nodeRef);
							} else if (archive) {
								// previews are only generated for the workspace store
								if (skipMimeTypeFix) logger.info("skipping preview generation for archived node:" + nodeRef);
								else refreshOriginalMimetype(nodeRef, name, "archived node " + nodeRef);
							} else if (skipMimeTypeFix) {
								if (execute) triggerThumbnailAction(nodeRef);
								else logger.info("protocol mode: would regenerate preview for " + nodeRef);
							} else {
								refreshMimetype(nodeRef, name);
							}
							if (fixVersionHistory) refreshVersionHistoryMimetypes(nodeRef);
						}  finally {
							policyBehaviourFilter.enableBehaviour(nodeRef);
						}
						nonPropagatingUserTransaction.commit();
					}catch (Throwable e) {
						logger.error(e.getMessage(),e);
						try {
							nonPropagatingUserTransaction.rollback();
						}catch (Exception e1) {
							logger.error("error rolling back transaction", e1);
						}
					}

				}
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * workspace node: fixes the mimetype metadata (no content rewrite) and regenerates the preview
	 * when the mimetype changed. The changed mimetype also changes the content hash
	 * (ContentData.hashCode), so the rendering service picks up the node again.
	 */
	private void refreshMimetype(NodeRef nodeRef, String name) {
		if (refreshOriginalMimetype(nodeRef, name, "workspace node " + nodeRef) == null) {
			return;
		}
		if (execute) {
			triggerThumbnailAction(nodeRef);
			logger.info("execute mode: regenerated preview for " + nodeRef);
		} else {
			logger.info("protocol mode: would regenerate preview for " + nodeRef);
		}
	}

	/**
	 * detects the mimetype of every version in the version history again and fixes the frozen metadata
	 * (cm:content mimetype and cclom:format). The binary is not rewritten (content url stays the same)
	 * and no preview is generated, because the preview is regenerated when a version gets reverted.
	 */
	private void refreshVersionHistoryMimetypes(NodeRef nodeRef) {
		VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
		if (versionHistory == null || versionHistory.getAllVersions() == null) {
			logger.info("no version history found for " + nodeRef);
			return;
		}
		for (Version version : versionHistory.getAllVersions()) {
			NodeRef frozenRef = version.getFrozenStateNodeRef();
			String name = (String) nodeService.getProperty(frozenRef, ContentModel.PROP_NAME);
			// frozen state refs point to the read only version store service, the converted ref allows writing the version node
			NodeRef versionNodeRef = VersionUtil.convertNodeRef(frozenRef);
			// the version node is not covered by the behaviour lock of the main node
			try {
				policyBehaviourFilter.disableBehaviour(versionNodeRef);
				refreshMimetypeMetadataOnly(frozenRef, versionNodeRef, name,
						"version " + version.getVersionLabel() + " of " + nodeRef);
			} finally {
				policyBehaviourFilter.enableBehaviour(versionNodeRef);
			}
		}
	}

	/**
	 * detects the mimetype again and only fixes the metadata (cm:content mimetype and cclom:format).
	 * The binary is not rewritten (content url stays the same) and no preview is generated.
	 * Used for all stores (workspace, versions, archived nodes); preview generation and disabling
	 * the policy behaviours for writableRef are the responsibility of the caller.
	 *
	 * @param readRef     ref used to read the content
	 * @param writableRef ref used to write the properties (may differ from readRef for the version store)
	 * @param label       description of the node for logging
	 * @return the new mimetype if it differs and was (or in protocol mode would be) changed, otherwise null
	 */
	private String refreshMimetypeMetadataOnly(NodeRef readRef, NodeRef writableRef, String name, String label) {
		ContentReader reader = contentService.getReader(readRef, ContentModel.PROP_CONTENT);
		if (reader == null || !reader.exists()) {
			logger.warn("no content found for " + label);
			return null;
		}
		String oldMimeType = reader.getMimetype();
		if (onlyInvalidMimetype && isValidMimetype(oldMimeType)) {
			return null;
		}

		String newMimetype = guessMimetype(reader, name);
		logger.debug(label + (writableRef.equals(readRef) ? "" : " (" + writableRef + ")")
				+ " oldMimeType:" + oldMimeType + " newMimeType:" + newMimetype);
		if (newMimetype == null || newMimetype.equals(oldMimeType)) {
			return null;
		}
		if (!execute) {
			logger.info("protocol mode: would set mimetype " + newMimetype + " (was: " + oldMimeType + ") for " + label);
			return newMimetype;
		}

		ContentData contentData = (ContentData) nodeService.getProperty(writableRef, ContentModel.PROP_CONTENT);
		if (contentData == null) {
			logger.warn("no content data found on " + writableRef + " (" + label + ")");
			return null;
		}
		logger.info("execute mode: set mimetype " + newMimetype + " (was: " + oldMimeType + ") for " + label);
		nodeService.setProperty(writableRef, ContentModel.PROP_CONTENT, ContentData.setMimetype(contentData, newMimetype));
		nodeService.setProperty(writableRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), newMimetype);
		return newMimetype;
	}

	/**
	 * fixes the mimetype metadata of an original (workspace or archived) node and syncs the changed
	 * cclom:format to its collection references. The sync is normally done by
	 * NodeCustomizationPolicies.onUpdateProperties, which is disabled while this job writes.
	 *
	 * @return the new mimetype if it was (or in protocol mode would be) changed, otherwise null
	 */
	private String refreshOriginalMimetype(NodeRef nodeRef, String name, String label) {
		Serializable oldFormat = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
		String newMimetype = refreshMimetypeMetadataOnly(nodeRef, nodeRef, name, label);
		if (newMimetype != null) {
			syncCollectionRefs(nodeRef, oldFormat, newMimetype, label);
		}
		return newMimetype;
	}

	/**
	 * syncs cclom:format to all collection references of the original. Like the policy, a ref is only
	 * updated when it still has the old value of the original (customized refs are kept).
	 */
	private void syncCollectionRefs(NodeRef original, Serializable oldFormat, String newMimetype, String label) {
		QName formatProp = QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT);
		for (NodeRef ref : NodeCustomizationPolicies.fetchCollectionReferencesByCmis(original)) {
			Serializable refFormat = nodeService.getProperty(ref, formatProp);
			if (!Objects.equals(Objects.toString(refFormat, null), Objects.toString(oldFormat, null))) {
				logger.info("skipping customized collection ref " + ref + " (cclom:format: " + refFormat + ") of " + label);
				continue;
			}
			if (!execute) {
				logger.info("protocol mode: would sync cclom:format " + newMimetype + " (was: " + refFormat + ") to collection ref " + ref + " of " + label);
				continue;
			}
			Map<QName, Serializable> before = new HashMap<>();
			before.put(formatProp, oldFormat);
			Map<QName, Serializable> after = new HashMap<>();
			after.put(formatProp, newMimetype);
			try {
				policyBehaviourFilter.disableBehaviour(ref);
				logger.info("execute mode: sync cclom:format " + newMimetype + " (was: " + refFormat + ") to collection ref " + ref + " of " + label);
				NodeCustomizationPolicies.syncCollectionRefProps(original, ref, before, after, true, nodeService);
			} catch (Exception e) {
				logger.error("error while syncing collection ref " + ref + " of " + label + ": " + e.getMessage(), e);
			} finally {
				policyBehaviourFilter.enableBehaviour(ref);
			}
		}
	}

	private String guessMimetype(ContentReader reader, String name) {
		//set to null so that guessMimetype() will not use old one
		reader.setEncoding(null);
		reader.setMimetype(null);
		return mimetypeService.guessMimetype(name, reader);
	}

	private String getMimetype(NodeRef nodeRef) {
		ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		return reader == null ? null : reader.getMimetype();
	}

	private static boolean isValidMimetype(String mimetype) {
		return mimetype != null && mimetype.contains("/");
	}

	private void triggerThumbnailAction(NodeRef nodeRef) {

		if(this.cleanUpExistingThumbnail){
			List<ChildAssociationRef> imgpreview = this.nodeService
					.getChildAssocs(nodeRef, RenditionModel.ASSOC_RENDITION, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "imgpreview"));
			if(imgpreview != null){
				if(imgpreview.size() == 0){
					logger.error("no preview found to delete.");
				}else if(imgpreview.size() > 1){
					logger.error("number of previews > 1. don't know which one to delete");
				}else {
					NodeRef thumbnail = imgpreview.get(0).getChildRef();
					logger.info("remove generated thumbnail: "+ thumbnail +" from io:" + nodeRef);
					new MCAlfrescoAPIClient().removeNode(thumbnail.getId(),null,false);
				}
			}
		}

		ActionService actionService = serviceRegistry.getActionService();
		Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
		thumbnailAction.setTrackStatus(true);

		thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);

		//cause its already async set executeAsynchronously to false
		actionService.executeAction(thumbnailAction, nodeRef, true, false);

		if(ActionStatus.Completed.equals(thumbnailAction.getExecutionStatus())){
			logger.info("action was successfull. trigger io update for elastic tracker");
			String name = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_NAME);
			nodeService.setProperty(nodeRef,ContentModel.PROP_NAME,name);
		}else{
			logger.error("action status." + thumbnailAction.getExecutionStatus() +" will not trigger update on io. tracker will not retrack preview");
		}
	}
}
