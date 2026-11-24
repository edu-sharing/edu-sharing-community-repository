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

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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
import org.apache.commons.lang3.StringUtils;
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
import org.edu_sharing.service.search.model.SortDefinition;
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

	@JobFieldDescription(description = "if true only thumbail generation will be done. mimetype fix will be skipped. default is false.")
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

	@JobFieldDescription(description = "number of nodes fetched per elastic search page (max 10000). default is 500.")
	Integer pageSize;

	@JobFieldDescription(description = "optional node id (uuid) to continue an aborted run: only nodes with a greater uuid are processed. The last node id of each page is logged.")
	String startAfterNodeId;

	private static final int DEFAULT_PAGE_SIZE = 500;

	/** fixes of the whole run, only committed nodes are added */
	private FixCounter fixed = new FixCounter();
	/** fixes of the node currently processed, added to {@link #fixed} after the commit */
	private FixCounter nodeFixed = new FixCounter();

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


			if (pageSize == null || pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE;
			// stay below the elastic max_result_window, larger pages would switch to a full scroll search
			pageSize = Math.min(pageSize, 10000);
			logger.info("using pageSize:" + pageSize);

			logger.info("using startAfterNodeId:" + startAfterNodeId);

			processPages();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * keyset paging over the node uuid: every page searches the nodes after the last processed uuid
	 * (sorted by uuid). Unlike offset paging this is stable when already fixed nodes drop out of the filter
	 * while the job is running, and it is not limited by the elastic max_result_window.
	 */
	private void processPages() {
		org.edu_sharing.service.search.SearchService localService = SearchServiceFactory.getLocalService();
		Query userFilter = QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(filter.getBytes()))).build()._toQuery();
		String lastNodeId = StringUtils.isBlank(startAfterNodeId) ? null : startAfterNodeId.trim();
		long total = -1;
		long processed = 0;
		int page = 0;
		fixed = new FixCounter();
		while (true) {
			final String after = lastNodeId;
			BoolQuery.Builder query = QueryBuilders.bool()
					.must(userFilter)
					// SortDefinition always sorts by type first, so restrict to one type to keep the uuid order stable
					.must(m -> m.term(t -> t.field("type").value(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO))));
			if (after != null) {
				query.must(m -> m.range(r -> r.term(t -> t.field("properties.sys:node-uuid").gt(after))));
			}

			SearchToken searchToken = new SearchToken();
			searchToken.setElasticQuery(query.build());
			searchToken.setFrom(0);
			searchToken.setMaxResult(pageSize);
			SortDefinition sort = new SortDefinition();
			sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry("sys:node-uuid", true));
			searchToken.setSortDefinition(sort);
			if (archive) {
				searchToken.setStoreProtocol(MCAlfrescoAPIClient.archiveStoreRef.getProtocol());
				searchToken.setStoreName(MCAlfrescoAPIClient.archiveStoreRef.getIdentifier());
			}

			SearchResultNodeRef search = localService.search(searchToken);
			List<org.edu_sharing.service.model.NodeRef> hits = search.getData();
			if (total < 0) {
				total = search.getNodeCount();
				logger.info("found:" + total);
			}
			if (hits == null || hits.isEmpty()) {
				break;
			}
			// every page starts at from=0, the range filter on the uuid moves the window. If the first hit is not
			// behind the last processed uuid, the range/sort did not work and the same page would be returned forever
			String firstNodeId = hits.get(0).getNodeId();
			if (after != null && firstNodeId.compareTo(after) <= 0) {
				logger.error("paging did not advance (first node id " + firstNodeId + " is not after " + after
						+ "), stopping to prevent an endless loop");
				break;
			}
			for (org.edu_sharing.service.model.NodeRef hit : hits) {
				processNode(new NodeRef(new StoreRef(hit.getStoreProtocol(), hit.getStoreId()), hit.getNodeId()));
				lastNodeId = hit.getNodeId();
			}
			processed += hits.size();
			logger.info("page " + page + ": " + hits.size() + " nodes, processed " + processed + " of " + total
					+ ", " + fixed + ", last node id: " + lastNodeId + " (use as startAfterNodeId to continue)");
			page++;
			// no break on a page smaller than pageSize: only an empty page ends the run, so a search
			// returning fewer hits than requested can not stop the job too early
		}
		logger.info("finished, processed " + processed + " nodes, " + fixed);
	}

	/**
	 * processes a single node in its own transaction
	 */
	private void processNode(NodeRef nodeRef) {
		QName typeQName = nodeService.getType(nodeRef);
		String type = typeQName.getLocalName();
		String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);

		logger.debug("name:" + name + " type:" + type);

		if (!typeQName.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
			return;
		}
		if (nodeService.getAspects(nodeRef).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
			logger.debug("ignoring collection_io_reference:" + nodeRef);
			return;
		}

		nodeFixed = new FixCounter();
		UserTransaction nonPropagatingUserTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		try {
			nonPropagatingUserTransaction.begin();
			try {
				policyBehaviourFilter.disableBehaviour(nodeRef);
				if (onlyInvalidMimetype && isValidMimetype(getMimetype(nodeRef))) {
					logger.debug("skipping node with valid mimetype:" + nodeRef);
				} else if (archive) {
					// previews are only generated for the workspace store
					if (skipMimeTypeFix) logger.info("skipping preview generation for archived node:" + nodeRef);
					else refreshOriginalMimetype(nodeRef, name, "archived node " + nodeRef);
				} else if (skipMimeTypeFix) {
					if (execute) triggerThumbnailAction(nodeRef);
					else logger.info("protocol mode: would regenerate preview for " + nodeRef);
					nodeFixed.previews++;
				} else {
					refreshMimetype(nodeRef, name);
				}
				if (fixVersionHistory) refreshVersionHistoryMimetypes(nodeRef);
			} finally {
				policyBehaviourFilter.enableBehaviour(nodeRef);
			}
			nonPropagatingUserTransaction.commit();
			// only count changes that were committed
			fixed.add(nodeFixed);
		} catch (Throwable e) {
			logger.error("error processing " + nodeRef + ": " + e.getMessage(), e);
			try {
				nonPropagatingUserTransaction.rollback();
			} catch (Exception e1) {
				logger.error("error rolling back transaction", e1);
			}
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
		nodeFixed.previews++;
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
				if (refreshMimetypeMetadataOnly(frozenRef, versionNodeRef, name,
						"version " + version.getVersionLabel() + " of " + nodeRef) != null) {
					nodeFixed.versions++;
				}
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
			nodeFixed.originals++;
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
				nodeFixed.collectionRefs++;
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
				nodeFixed.collectionRefs++;
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

	/**
	 * counts the fixed nodes. In protocol mode it counts the nodes that would be fixed.
	 */
	private class FixCounter {
		long originals;
		long collectionRefs;
		long versions;
		long previews;

		void add(FixCounter other) {
			originals += other.originals;
			collectionRefs += other.collectionRefs;
			versions += other.versions;
			previews += other.previews;
		}

		@Override
		public String toString() {
			return (execute ? "fixed" : "would be fixed (protocol mode)") + " originals: " + originals
					+ ", collection refs: " + collectionRefs + ", versions: " + versions + ", previews: " + previews;
		}
	}
}
