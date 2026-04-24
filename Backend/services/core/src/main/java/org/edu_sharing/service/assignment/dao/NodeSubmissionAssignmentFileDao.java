package org.edu_sharing.service.assignment.dao;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFile;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.util.CheckedFunction;
import org.edu_sharing.util.LazyProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@Slf4j
final class NodeSubmissionAssignmentFileDao extends BasicNodeDaoImpl implements AssignmentFileDao {
    private final NodeSubmissionAssignmentDao assignmentDao;
    private final LazyProvider<Node> referNode;

    @Setter(onMethod_ = @Autowired)
    private PermissionService permissionService;

    public NodeSubmissionAssignmentFileDao(NodeSubmissionAssignmentDao assignmentDao, String nodeId) {
        super(nodeId);
        this.assignmentDao = assignmentDao;


        referNode = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return Optional.ofNullable(propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO))
                    .map(CheckedFunction.wrap(n -> NodeDao.getNode(n).asNode()))
                    .orElse(null);
        }));
    }


    @Override
    @RunAsSystem
    @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void create(AssignmentFileRequest request) {
        if (StringUtils.isNotBlank(nodeId)) {
            throw new IllegalStateException("AssignmentFile with id " + nodeId + " already exists.");
        }

        validateCanChangeAssignment();

        log.debug("Creating new assignment file");
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, request.documentRole().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.refId()));
        }};

        nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, assignmentDao.getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE, CCConstants.CCM_ASSOC_ASSIGNMENT_FILES, properties);
        nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
        permissionService.setPermissionInherit(nodeId, true);
        log.debug("Created new assignment file {} to {}", nodeId, assignmentDao.getNodeId());
        handleReferenceCopy(request, null, properties);
        log.debug("Updated properties for new assignment file {} with {}", nodeId, properties);
        nodeService.updateNodeNative(nodeId, properties);
    }

    @Override
    public AssignmentFile getAssignmentFile() {
        if (!exists()) {
            return null;
        }

        return new AssignmentFile(
                getNodeRef(),
                referNode.get(),
                getDocumentRole(),
                null);
    }

    @Override
    public String getReferNodeId() {
        return propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO).getId();
    }


    @Override
    @RunAsSystem
    @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void update(@NonNull AssignmentFileRequest assignmentFileRequest) {
        validateExists();

        validateCanChangeAssignment();

        Map<String, Object> properties = new HashMap<>();
        if (!Objects.equals(assignmentFileRequest.refId(), getReferNodeId())) {
            handleReferenceCopy(assignmentFileRequest, getReferNodeId(), properties);
            properties.putIfAbsent(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, new NodeRef(StoreRef.PROTOCOL_WORKSPACE, assignmentFileRequest.refId()));
        }

        if (!Objects.equals(assignmentFileRequest.documentRole(), getDocumentRole())) {
            properties.put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, assignmentFileRequest.documentRole().name());
        }

        if (!properties.isEmpty()) {
            nodeService.updateNodeNative(nodeId, properties);
            refresh();
        }
    }

    @Override
    public Boolean isDone() {
        return null;
    }

    @Override
    @RunAsSystem
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void delete() {
        if (!exists()) {
            return;
        }
        validateCanChangeAssignment();
        doDelete();
        refresh();
    }

    private void handleReferenceCopy(@NotNull AssignmentFileRequest assignmentFileRequest, String currentReferNodeId, Map<String, Object> properties) {
        if (StringUtils.isNotBlank(currentReferNodeId) && nodeService.exists(currentReferNodeId)) {
            log.debug("Deleting old reference node {}", currentReferNodeId);
            nodeService.removeNode(currentReferNodeId, nodeId, false);
        }

        if (Boolean.parseBoolean(nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), assignmentFileRequest.refId(), CCConstants.CCM_PROP_RESTRICTED_ACCESS))) {
            log.debug("Skipping reference copy for restricted access document");
            return;
        }

        log.debug("Copying reference node {}", assignmentFileRequest.refId());
        org.alfresco.service.cmr.repository.NodeRef nodeRef = nodeService.copyNode(assignmentFileRequest.refId(), nodeId, CCConstants.CCM_ASSOC_ASSIGNMENT_FILE_COPY, true);
        nodeService.setOwner(nodeRef.getId(), ApplicationInfoList.getHomeRepository().getUsername());
        log.debug("Copied reference node {}", nodeRef.getId());
        properties.put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, nodeRef);
        nodeService.addAspect(nodeRef.getId(), CCConstants.CCM_ASPECT_ASSIGNMENT_FILE_DEEP_COPY);
        log.debug("Added deep copy aspect to reference node {}", nodeRef.getId());
    }

    @Override
    public AssignmentFile.Role getDocumentRole() {
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, AssignmentFile.Role.class);
    }

    private void validateCanChangeAssignment() {
        if(!assignmentDao.canChangeAssignment()){
            throw new IllegalStateException("Cannot edit assignment for assignment with type " + assignmentDao.getType());
        }
    }
}
