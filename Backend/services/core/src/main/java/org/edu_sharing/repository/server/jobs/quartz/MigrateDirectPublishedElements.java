package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.service.handleservice.HandleServiceFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.HandleMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Migrate previously directly published element to published copies")
public class MigrateDirectPublishedElements extends AbstractJobMapAnnotationParams{

	@JobFieldDescription(description = "Single node to migrate")
	private String nodeId;
	@JobFieldDescription(description = "Folder to migrate (if unset, a query for all relevant elements is processed)")
	private String startFolder;
	@JobFieldDescription(description = "nodes to explicitly exclude")
	private List<String> ignoredNodeIds;

	@Autowired
	private NodeService nodeService;
	@Autowired
	private BehaviourFilter policyBehaviourFilter;
	@Autowired
	private RepositoryCache repositoryCache;
	@Autowired
	private PermissionService permissionService;

	@Autowired
	private RetryingTransactionHelper retryingTransactionHelper;

	@Autowired
	HandleServiceFactory handleServiceFactory;

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
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
		if(StringUtils.isBlank(startFolder)) {
            runner.setElastic("{\"bool\":{\"minimum_should_match\":1,\"should\":[{\"exists\":{\"field\":\"properties.ccm:published_handle_id\"}},{\"exists\":{\"field\":\"properties.ccm:published_doi_id\"}}],\"must_not\":[{\"exists\":{\"field\":\"properties.ccm:published_original\"}},{\"term\":{\"aspects\":\"ccm:collection_io_reference\"}}]}}");
		} else {
			runner.setStartFolder(startFolder);
		}
		runner.setKeepModifiedDate(false);
		runner.setTransaction(NodeRunner.TransactionMode.None);
		int count=runner.run();
        log.info("Processed {} nodes", count);
	}

	private void migrate(NodeRef ref) {
		if(ignoredNodeIds != null && ignoredNodeIds.contains(ref.getId())) {
            log.warn("Node {} shall be ignored", ref);
			return;
		}
		Serializable handleId = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
		Serializable doiId = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_PUBLISHED_DOI_ID);
		if(handleId == null && doiId == null) {
            log.warn("Can not migrate node {} since it has no handle id", ref);
			return;
		}
		if(NodeServiceHelper.hasAspect(ref, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
            log.warn("Can not migrate node {} since it is a ref", ref);
			return;
		}
		if(NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL) != null) {
            log.warn("Can not migrate node {} since it is a published copy", ref);
			return;
		}
		// copy the old publish date
		Serializable date = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_DATE);
		if (date != null) {
            log.info("Keeping old published date {}", date);
		} else {
			log.warn("Old node had no published date! Will use cm:modified as fallback");
			date = NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_MODIFIED);
		}
		try {
			// do not do anything with the handle for now!
            log.info("Creating published copy of {}", ref);
			NodeRef copy = retryingTransactionHelper.doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(ref);
				NodeRef copyInternal = new NodeRef(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
						nodeService.publishCopy(ref.getId(), null));
				policyBehaviourFilter.enableBehaviour(ref);
				return copyInternal;
			});

			if(!NodeServiceHelper.exists(copy)) {
                log.error("Copy failed for node: {}, missing node id: {}", ref, copy);
				return;
			}
            log.info("Created copy: {}", copy);
			Serializable finalDate = date;
			retryingTransactionHelper.doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(copy);
				NodeServiceHelper.setProperty(copy, CCConstants.CM_PROP_C_CREATED, NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_CREATED), true);
				NodeServiceHelper.setProperty(copy, CCConstants.CM_PROP_C_MODIFIED, NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_MODIFIED), true);
				// now, fake the current history of copies to the directly published element so its handle id gets the update
				if(handleId != null) {
                    log.info("Update old handle {} from {} to {}", handleId, ref, copy);
					nodeService.createHandle(copy, Collections.singletonList(ref.getId()), handleServiceFactory.instance(HandleServiceFactory.IMPLEMENTATION.handle), HandleMode.update);
				}
				if(doiId != null) {
                    log.info("Update old handle {} from {} to {}", doiId, ref, copy);
					nodeService.createHandle(copy, Collections.singletonList(ref.getId()), handleServiceFactory.instance(HandleServiceFactory.IMPLEMENTATION.doi), HandleMode.update);
				}
				// copy the old publish date
				if (finalDate != null) {
					NodeServiceHelper.setProperty(copy, CCConstants.CCM_PROP_IO_PUBLISHED_DATE, finalDate, true);
				}
				policyBehaviourFilter.enableBehaviour(copy);
				return null;
			});
			retryingTransactionHelper.doInTransaction(() -> {
						policyBehaviourFilter.disableBehaviour(ref);
						Set<AccessPermission> perm = permissionService.getAllSetPermissions(ref).
								stream().filter(p -> p.getAccessStatus().equals(AccessStatus.ALLOWED) && p.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE)).collect(Collectors.toSet());
						log.info("cleaning up permissions");
						if (perm.stream().anyMatch(p -> p.getPermission().equals(CCConstants.PERMISSION_CONSUMER))) {
							permissionService.deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CONSUMER);
						}
						if (perm.stream().anyMatch(p -> p.getPermission().equals(CCConstants.PERMISSION_CC_PUBLISH))) {
							permissionService.deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CC_PUBLISH);
						}
						NodeServiceHelper.removeProperty(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
						policyBehaviourFilter.enableBehaviour(ref);
						return null;
					});
            log.info("done for node: {}, new copy: {}", ref, copy);
			repositoryCache.remove(ref.getId());

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}
}
