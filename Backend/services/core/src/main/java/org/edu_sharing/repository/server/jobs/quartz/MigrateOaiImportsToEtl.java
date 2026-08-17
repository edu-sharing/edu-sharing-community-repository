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
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.tika.utils.StringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.bulk.BulkServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Semaphore;

@JobDescription(description = "Migrate nodes previously imported via OAI (IMP_OBJ) to nodes which will should be processed by the etl-framework. " +
		"Note: cclom:version is increased by 3 minor versions since the version history is rebuilt; cm:modified/cm:modifier are preserved")
public class MigrateOaiImportsToEtl extends AbstractInterruptableJob{
	protected Logger logger = Logger.getLogger(MigrateOaiImportsToEtl.class);
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	NodeService nodeService = serviceRegistry.getNodeService();

	BehaviourFilter policyBehaviourFilter = applicationContext.getBean("policyBehaviourFilter", BehaviourFilter.class);

	RepositoryCache repositoryCache = applicationContext.getBean(RepositoryCache.class);

	@JobFieldDescription(description = "Set Id (folder name) of the IMP_OBJ set to migrate")
	private String setId;
	@JobFieldDescription(description = "Id of the spider that this set should now belong to (i.e. oeh_spider, will be used for the crawler folder - defaults to sourceId)")
	private String spiderId;
	@JobFieldDescription(description = "Source id to set into ccm:replicationsource")
	private String sourceId;
	@JobFieldDescription(description = "If the id should be transformed (copied from an other field into ccm:replicationsourceuuid), enter it here ")
	private String propertyId;
	@JobFieldDescription(description = "When set and not empty, only this node will be transformed (for testing)")
	private String testNodeId;
	@JobFieldDescription(description = "Skip currently marked deleted elements? (ccm:editorial_state == deleted)", sampleValue = "true")
	private Boolean skipDeleted;
	@JobFieldDescription(description = "Run the job threaded", sampleValue = "true")
	private Boolean threaded;

	NodeRef startFolder;
	String target;
	private static final Semaphore semaphore = new Semaphore(1);

	@Override
	public void executeInterruptable(JobExecutionContext context) throws JobExecutionException {
		if(setId == null) {
			throwMissingParam("setId");
		}
		if(sourceId == null) {
			throwMissingParam("sourceId");
		}
		if(spiderId == null) {
			spiderId = sourceId;
		}
		AuthenticationUtil.runAsSystem(() -> {
			try {
				String importFolder = PersistentHandlerEdusharing.prepareImportFolder();
				startFolder = NodeServiceFactory.getInstance().getLocalService().getChild(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, importFolder,
						CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
						setId
				);
				Map<QName, Serializable> props = new HashMap<>();
				props.put(ContentModel.PROP_NAME, spiderId);
				target = NodeServiceFactory.getInstance().getLocalService().findNodeByName(
						BulkServiceFactory.getInstance().getPrimaryFolder().getId(),
						spiderId
				);
				if(target == null) {
					target = nodeService.createNode(
							BulkServiceFactory.getInstance().getPrimaryFolder(),
							ContentModel.ASSOC_CONTAINS,
							QName.createQName(spiderId),
							QName.createQName(CCConstants.CCM_TYPE_MAP),
							props
					).getChildRef().getId();
				}
				/*String migration = NodeServiceFactory.getLocalService().findNodeByName(
						BulkServiceFactory.getInstance().getPrimaryFolder().getId(),
						"MIGRATION"
				);
				if(migration == null) {
					props = new HashMap<>() {{
						put(ContentModel.PROP_NAME, "MIGRATION");
					}};
					target = nodeService.createNode(
							BulkServiceFactory.getInstance().getPrimaryFolder(),
							ContentModel.ASSOC_CONTAINS,
							QName.createQName("MIGRATION"),
							QName.createQName(CCConstants.CCM_TYPE_MAP),
							props
					).getChildRef().getId();
				} else {
					target = migration;
				}*/
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
			if (testNodeId != null && !testNodeId.trim().isEmpty()) {
				serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
					this.transform(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, testNodeId));
					return null;
				});
			} else {
				NodeRunner runner = new NodeRunner();

				runner.setTask(this::transform);
				runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
				runner.setRunAsSystem(true);
				runner.setThreaded(threaded != null && threaded);
				runner.setRecurseMode(RecurseMode.Folders);
				runner.setStartFolder(startFolder.getId());
				runner.setKeepModifiedDate(true);
				runner.setTransaction(NodeRunner.TransactionMode.Local);
				int count = runner.run();
				logger.info("Processed " + count + " nodes");
			}
			return null;
		});
	}

	private synchronized void transform(NodeRef nodeRef) {
		if(isInterrupted()) {
			return;
		}
		AuthenticationUtil.runAsSystem(() -> {
			transformInternal(nodeRef);
			return null;
		});

	}

	private void transformInternal(NodeRef nodeRef) {
		logger.debug("Bulk transform node " + nodeRef.getId() + " started");
		if (skipDeleted != null && skipDeleted) {
			Serializable state = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_EDITORIAL_STATE));
			if ("deleted".equals(state)) {
				logger.info("Node " + nodeRef + " is marked as deleted. Will not migrate. " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
				return;
			}
		}
		/*
		 * the state of the node before any write happens:
		 * - cm:modified/cm:modifier will be restored at the end (the version revert writes the
		 *   frozen values and the createVersion policies touch the node otherwise)
		 * - properties which are unset now must not end up as empty collections: the frozen node of a
		 *   version carries every property of the type/aspect definition, and unset multivalue
		 *   properties are read back as an empty list which the revert then writes to the live node
		 */
		Map<QName, Serializable> propsBefore = nodeService.getProperties(nodeRef);
		Serializable modifiedBefore = propsBefore.get(ContentModel.PROP_MODIFIED);
		Serializable modifierBefore = propsBefore.get(ContentModel.PROP_MODIFIER);
		// keep cm:modified/cm:modifier: only with the auditable behaviour disabled the values we pass
		// in at the end are taken literally instead of being replaced by "now"
		boolean auditableDisabledByUs = policyBehaviourFilter.isEnabled(nodeRef, ContentModel.ASPECT_AUDITABLE);
		if (auditableDisabledByUs) {
			policyBehaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
		}
		try {
			if (transformNode(nodeRef, propsBefore)) {
				if (modifiedBefore != null) {
					nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIED, modifiedBefore);
				}
				if (modifierBefore != null) {
					nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIER, modifierBefore);
				}
			}
		} finally {
			if (auditableDisabledByUs) {
				policyBehaviourFilter.enableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
			}
			// the permalink & preview url are built from the (now changed) version, so drop any cached state
			repositoryCache.remove(nodeRef.getId());
		}
	}

	/**
	 * @return true if the node was actually migrated, false if it was skipped
	 */
	private boolean transformNode(NodeRef nodeRef, Map<QName, Serializable> propsBefore) {
		if (propertyId != null && !propertyId.trim().isEmpty()) {
			Serializable newId = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.getValidGlobalName(propertyId)));
			if (newId == null) {
				logger.warn("Node " + nodeRef + " has no data for the new property id in field " + propertyId + ", will not move this node. Check it and migrate it later. " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
				return false;
			} else {
				if (newId instanceof Collection) {
					newId = (Serializable) ((Collection<?>) newId).iterator().next();
				}
				if(newId instanceof String && StringUtils.isBlank((String) newId)) {
					logger.warn("Node " + nodeRef + " has empty string ata for the new property id in field " + propertyId + ", will not move this node. Check it and migrate it later. " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
					return false;
				}
				nodeService.setProperty(
						nodeRef,
						QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEUUID),
						newId
				);
			}
			logger.info("Bulk transforming node replication id " + nodeRef.getId() + " " + newId + " " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
		}

		nodeService.setProperty(
				nodeRef,
				QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE),
				sourceId
		);
		if(threaded != null && threaded) {
			semaphore.acquireUninterruptibly();
		}
		String parentName = setId + "_" + NodeServiceHelper.getProperty(NodeServiceHelper.getPrimaryParent(nodeRef), CCConstants.CM_NAME);
		String groupedTarget = NodeServiceFactory.getInstance().getLocalService().findNodeByName(
				target,
				parentName
		);
		if (groupedTarget == null) {
			Map<QName, Serializable> props = new HashMap<>() {{
				put(ContentModel.PROP_NAME, parentName);
			}};
			groupedTarget = nodeService.createNode(
					new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, target),
					ContentModel.ASSOC_CONTAINS,
					QName.createQName(parentName),
					QName.createQName(CCConstants.CCM_TYPE_MAP),
					props
			).getChildRef().getId();
		}
		if(threaded != null && threaded) {
			semaphore.release();
		}
		NodeServiceFactory.getInstance().getLocalService().moveNode(groupedTarget, CCConstants.CM_ASSOC_FOLDER_CONTAINS, nodeRef.getId());
		try {
			// hold the latest state of the object, i.e. user modificationns
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_MIGRATION
			);
			org.edu_sharing.service.nodeservice.NodeService service = NodeServiceFactory.getInstance().getLocalService();
			service.createVersion(nodeRef.getId());
			VersionHistory history = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
			// revert to the initial version of the import
			service.revertVersionNoRollback(nodeRef.getId(), history.getRootVersion().getVersionLabel());
			// tag it as it was the bulk_create event so the crawler can detect modifications
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_CREATE
			);
			String oldVersion = service.createVersion(nodeRef.getId());
			// finally, rollback the version with all changes and at it on top
			service.revertVersionNoRollback(nodeRef.getId(), history.getHeadVersion().getVersionLabel());
			// drop empty collections the revert introduced for properties that were unset before,
			// so the final version freezes the cleaned up state
			removeEmptyPropertiesIntroducedByRevert(nodeRef, propsBefore);
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_MIGRATION
			);
			String newVersion = service.createVersion(nodeRef.getId());
			// finally, delete all other versions
			Collection<Version> newHistory = serviceRegistry.getVersionService().getVersionHistory(nodeRef).getAllVersions();
			for (Version version : newHistory) {
				if (!Arrays.asList(oldVersion, newVersion).contains(version.getVersionLabel())) {
					serviceRegistry.getVersionService().deleteVersion(nodeRef, version);
				}
			}
			logger.info("Bulk transform fully finished for " + nodeRef.getId() + " ");
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * removes properties which are an empty collection now but had no value before the migration
	 * (values which already were an empty collection before are left untouched)
	 */
	private void removeEmptyPropertiesIntroducedByRevert(NodeRef nodeRef, Map<QName, Serializable> propsBefore) {
		nodeService.getProperties(nodeRef).forEach((property, value) -> {
			if (value instanceof Collection<?> && ((Collection<?>) value).isEmpty()
					&& propsBefore.get(property) == null) {
				logger.debug("Removing empty property " + property.getLocalName() + " of node " + nodeRef.getId());
				nodeService.removeProperty(nodeRef, property);
			}
		});
	}

	private void throwMissingParam(String param) {
        String message = "Missing required parameter(s) '" + param + "'";
        logger.error(message);
        throw new IllegalArgumentException(message);
	}

	public void run() {

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
