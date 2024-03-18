package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Collections;

@JobDescription(description = "Migrate previously directly published element to published copies")
public class MigrateDirectPublishedElements extends AbstractJobMapAnnotationParams{

	protected Logger logger = Logger.getLogger(MigrateDirectPublishedElements.class);


	@JobFieldDescription(description = "Single node to migrate")
	private String nodeId;
	private NodeService nodeService;
	private BehaviourFilter policyBehaviourFilter;
	private ServiceRegistry serviceRegistry;

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
		nodeService = NodeServiceFactory.getLocalService();
		policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
		if(!StringUtils.isBlank(nodeId)) {
			AuthenticationUtil.runAsSystem(() -> {
				migrate(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
				return null;
			});
			return;
		}
		NodeRunner runner = new NodeRunner();
		runner.setTask(this::migrate);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setLucene("ISNOTNULL:\"ccm:published_handle_id\" AND ISNULL:\"ccm:published_original\" AND NOT ASPECT:\"ccm:collection_io_reference\"");
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	private void migrate(NodeRef ref) {
		Serializable handleId = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
		if(handleId == null) {
			logger.warn("Can not migrate node " + ref + " since it has no handle id");
			return;
		}
		if(NodeServiceHelper.hasAspect(ref, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
			logger.warn("Can not migrate node " + ref + " since it is a ref");
			return;
		}
		if(NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL) != null) {
			logger.warn("Can not migrate node " + ref + " since it is a published copy");
			return;
		}
		try {
			// do not do anything with the handle for now!
			NodeRef copy = new NodeRef(
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
					nodeService.publishCopy(ref.getId(), null));
			serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(copy);
				// now, fake the current history of copies to the directly published element so its handle id gets the update
				nodeService.createHandle(copy, Collections.singletonList(ref.getId()), HandleMode.update);

				// copy the old publish date
				Serializable date = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_DATE);
				if (date != null) {
					NodeServiceHelper.setProperty(copy, CCConstants.CCM_PROP_IO_PUBLISHED_DATE, date, true);
				}
				serviceRegistry.getPermissionService().deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CONSUMER);
				serviceRegistry.getPermissionService().deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CC_PUBLISH);
				policyBehaviourFilter.enableBehaviour(copy);
				return null;
			});
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		NodeServiceHelper.removeProperty(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
		NodeServiceHelper.removeAspect(ref, CCConstants.CCM_ASPECT_PUBLISHED);
	}
}
