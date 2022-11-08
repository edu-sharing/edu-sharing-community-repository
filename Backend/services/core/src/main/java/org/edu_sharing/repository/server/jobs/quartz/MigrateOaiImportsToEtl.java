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
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.bulk.BulkServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;

@JobDescription(description = "Migrate nodes previously imported via OAI (IMP_OBJ) to nodes which will should be processed by the etl-framework")
public class MigrateOaiImportsToEtl extends AbstractJob{
	protected Logger logger = Logger.getLogger(MigrateOaiImportsToEtl.class);
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	NodeService nodeService = serviceRegistry.getNodeService();

	@JobFieldDescription(description = "Set Id (folder name) of the IMP_OBJ set to migrate")
	private String setId;
	@JobFieldDescription(description = "Id of the spider that this set should now belong to (i.e. oeh_spider)")
	private String spiderId;
	@JobFieldDescription(description = "If the id should be transformed (copied from an other field into ccm:replicationsourceid), enter it here ")
	private String propertyId;
	@JobFieldDescription(description = "When set and not empty, only this node will be transformed (for testing)")
	private String testNodeId;

	NodeRef startFolder;
	String target;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {


		setId = prepareParam(context, "setId", true);
		spiderId = prepareParam(context, "spiderId", true);
		testNodeId = prepareParam(context, "testNodeId", false);
		propertyId = prepareParam(context, "propertyId", false);
		AuthenticationUtil.runAsSystem(() -> {
			try {
				String importFolder = PersistentHandlerEdusharing.prepareImportFolder();
				startFolder = NodeServiceFactory.getLocalService().getChild(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, importFolder,
						CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
						setId
				);
				Map<QName, Serializable> props = new HashMap<>();
				props.put(ContentModel.PROP_NAME, spiderId);
				target = NodeServiceFactory.getLocalService().findNodeByName(
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
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
			if (testNodeId != null && !testNodeId.trim().isEmpty()) {
				this.transform(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, testNodeId));
			} else {
				NodeRunner runner = new NodeRunner();

				runner.setTask(this::transform);
				runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
				runner.setRunAsSystem(true);
				runner.setThreaded(false);
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
		logger.info("Bulk transform node " + nodeRef.getId());
		nodeService.setProperty(
				nodeRef,
				QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE),
				spiderId
		);
		if(propertyId != null && !propertyId.trim().isEmpty()) {
			Serializable newId = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.getValidGlobalName(propertyId)));
			if(newId == null) {
				logger.warn("Node " + nodeRef + " has no data for the new property id in field " + propertyId +", will not override current id " + nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
			} else {
				nodeService.setProperty(
						nodeRef,
						QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID),
						newId
				);
			}
		}
		nodeService.setProperty(
				nodeRef,
				QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE),
				spiderId
		);
		NodeServiceFactory.getLocalService().moveNode(target, CCConstants.CM_ASSOC_FOLDER_CONTAINS, nodeRef.getId());
		try {
			// hold the latest state of the object, i.e. user modificationns
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_MIGRATION
			);
			NodeServiceFactory.getLocalService().createVersion(nodeRef.getId());
			VersionHistory history = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
			// revert to the initial version of the import
			NodeServiceFactory.getLocalService().revertVersion(nodeRef.getId(), history.getRootVersion().getVersionLabel());
			// tag it as it was the bulk_create event so the crawler can detect modifications
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_CREATE
			);
			NodeServiceFactory.getLocalService().createVersion(nodeRef.getId());
			// finally, rollback the version with all changes and at it on top
			NodeServiceFactory.getLocalService().revertVersion(nodeRef.getId(), history.getHeadVersion().getVersionLabel());
			nodeService.setProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT),
					CCConstants.VERSION_COMMENT_BULK_MIGRATION
			);
			NodeServiceFactory.getLocalService().createVersion(nodeRef.getId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private String prepareParam(JobExecutionContext context, String param, boolean required) {
		String value = (String) context.getJobDetail().getJobDataMap().get(param);
		if(value==null && required) {
			throwMissingParam(param);
		}
		return value;

	}

	private void throwMissingParam(String param) {
		throw new IllegalArgumentException("Missing required parameter(s) '" + param + "'");
	}

	public void run() {

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
