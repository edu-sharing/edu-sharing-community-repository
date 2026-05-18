package org.edu_sharing.service.assignment.dao;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFile;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.SubmissionFileDao;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.transform.RepresentationService;
import org.edu_sharing.util.CheckedFunction;
import org.edu_sharing.util.CheckedSupplier;
import org.edu_sharing.util.LazyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
final class NodeSubmissionFileDao extends BasicNodeDaoImpl implements SubmissionFileDao {

    private final NodeSubmissionAssignmentDao assignmentDao;
    private final NodeSubmissionDao submissionDao;

    private final LazyProvider<Optional<NodeRef>> contentNodeRef;
    private final LazyProvider<Optional<NodeRef>> correctionNodeRef;

    private final LazyProvider<Node> contentNode;
    private final LazyProvider<Node> correctionNode;

    @Setter(onMethod_ = @Autowired)
    private PermissionService permissionService;

    @Setter(onMethod_ = @Autowired)
    private RepresentationService representationService;

    private final MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();


    public NodeSubmissionFileDao(NodeSubmissionAssignmentDao assignmentDao, NodeSubmissionDao submissionDao, String nodeId) {
        super(nodeId);
        this.assignmentDao = assignmentDao;
        this.submissionDao = submissionDao;

        contentNodeRef = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_IO)
                    .stream()
                    .filter(x -> x.getTypeQName().toString().equals(CCConstants.CCM_ASSOC_SUBMISSION_FILE_CONTENT))
                    .map(ChildAssociationRef::getChildRef)
                    .findFirst());
        }));

        correctionNodeRef = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_IO)
                    .stream()
                    .filter(x -> x.getTypeQName().toString().equals(CCConstants.CCM_ASSOC_SUBMISSION_FILE_CORRECTION))
                    .map(ChildAssociationRef::getChildRef)
                    .findFirst());
        }));

        contentNode = registerLazyProvider(new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            return contentNodeRef.get()
                    .map(CheckedFunction.wrap(n -> NodeDao.getNode(n).asNode()))
                    .orElse(null);
        })));

        correctionNode = registerLazyProvider(new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            return correctionNodeRef.get()
                    .map(CheckedFunction.wrap(n -> NodeDao.getNode(n).asNode()))
                    .orElse(null);
        })));
    }

    @Override
    @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void create(SubmissionFileRequest request, InputStream fileInputStream) {
        submissionDao.validateAssigneeCanChangeSubmission();

        if (!assignmentDao.getAllowAdditionalDocumentSubmissions() && submissionDao.getSubmissionFiles()
                .stream()
                .anyMatch(x -> x.getReferToAssigmentFile()
                        .map(org.edu_sharing.service.assignment.BasicNodeDao::getNodeId)
                        .map(y -> y.equals(request.assignmentFile()))
                        .orElse(false))) {
            throw new IllegalArgumentException("A submission file to the same assignment file already exists.");
        }

        if (StringUtils.isNotBlank(nodeId)) {
            throw new IllegalStateException("Submission file id must be empty, but is: " + nodeId);
        }
        if (!assignmentDao.getAllowAdditionalDocumentSubmissions() || request.assignmentFile() != null) {
            // validates if assignment file exists otherwise throws exception
            assignmentDao.getAssignmentFile(request.assignmentFile());
        }
        log.debug("Creating new submission file");
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
        }};
        if (request.assignmentFile() != null) {
            properties.put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FILE, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.assignmentFile()));
        }

        AuthenticationUtil.runAsSystem(() -> {
            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, submissionDao.getNodeId(), CCConstants.CCM_TYPE_SUBMISSION_FILE, CCConstants.CCM_ASSOC_SUBMISSION_FILES, properties);
            nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
            return null;
        });

        log.debug("Created new submission file for {} to {}", nodeId, submissionDao.getNodeId());
        handleSubmissionFile(request, fileInputStream);
        refresh();
        AuthenticationUtil.runAsSystem(() -> {

            NodeRef nodeRef = representationService.updateChildPdf(getAlfrescoContentNodeRef(), getAlfrescoNodeRef(), getAlfrescoCorrectionNodeRef(), "correction", CCConstants.CCM_TYPE_IO, CCConstants.CCM_ASSOC_SUBMISSION_FILE_CORRECTION);
            if (nodeRef != null) {
                nodeService.setOwner(nodeRef.getId(), ApplicationInfoList.getHomeRepository().getUsername());
            }
            return null;
        });

        refresh();
    }

    @Override
    @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void updateCorrectionFile(InputStream fileInputStream) {
        refresh();
        validateExists();

        submissionDao.validateCanCoordinatorChangeSubmission();

        AuthenticationUtil.runAsSystem(() -> {
            NodeRef alfrescoCorrectionNodeRef = getAlfrescoCorrectionNodeRef();
            if (alfrescoCorrectionNodeRef == null) {
                log.debug("Creating new correction node for {}", nodeId);
                nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, submissionDao.getNodeId(), CCConstants.CCM_TYPE_IO, CCConstants.CCM_ASSOC_SUBMISSION_FILE_CORRECTION, Map.of(CCConstants.CM_NAME, "correction"));
                nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
                alfrescoCorrectionNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            }
            apiClient.writeContent(alfrescoCorrectionNodeRef.getStoreRef(), alfrescoCorrectionNodeRef.getId(), fileInputStream, null, null, CCConstants.CM_PROP_CONTENT);
            return null;
        });
    }


    private void handleSubmissionFile(SubmissionFileRequest request, InputStream fileInputStream) {
        if (StringUtils.isNotBlank(request.originalFile()) && fileInputStream != null) {
            throw new IllegalArgumentException("Cannot create submission file with original file and file input stream");
        }

        if (StringUtils.isNotBlank(getContentNodeId_Internal()) && nodeService.exists(getContentNodeId_Internal())) {
            log.debug("Deleting old content node {}", getContentNodeId());
            AuthenticationUtil.runAsSystem(() -> {
                nodeService.removeNode(getContentNodeId(), nodeId, false);
                return null;
            });
        }

        if (StringUtils.isNotBlank(request.originalFile())) {
            handleReferenceCopy(request);
        } else {
            handleFileUpload(request, fileInputStream);
        }
    }

    private void handleReferenceCopy(SubmissionFileRequest request) {

        if (NodeServiceHelper.hasRestrictedAccess(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.originalFile()), CCConstants.PERMISSION_READ_ALL, CCConstants.PERMISSION_DOWNLOAD_CONTENT)) {
            log.debug("Skipping reference copy for restricted access document");
            return;
        }

        org.alfresco.service.cmr.repository.NodeRef originalNode = nodeService.getOriginalNode(request.originalFile());
        if (!permissionService.hasPermission(originalNode.getStoreRef().getProtocol(), originalNode.getStoreRef().getIdentifier(), originalNode.getId(), CCConstants.PERMISSION_READ_ALL)) {
            throw new InsufficientPermissionException("You do not have permission to copy the original file. Required permission: " + CCConstants.PERMISSION_READ_ALL);
        }

        if (!permissionService.hasPermission(originalNode.getStoreRef().getProtocol(), originalNode.getStoreRef().getIdentifier(), originalNode.getId(), CCConstants.PERMISSION_DOWNLOAD_CONTENT)) {
            throw new InsufficientPermissionException("You do not have permission to copy the original file. Required permission: " + CCConstants.PERMISSION_DOWNLOAD_CONTENT);
        }

        String currentUser = AuthenticationUtil.getRunAsUser();
        AuthenticationUtil.runAsSystem(() -> {
            log.debug("Copying reference node {}", originalNode.getId());
            NodeRef contentNodeRef = nodeService.copyNode(originalNode.getId(), nodeId, CCConstants.CCM_ASSOC_SUBMISSION_FILE_CONTENT, true, null);
            nodeService.setOwner(contentNodeRef.getId(), ApplicationInfoList.getHomeRepository().getUsername());
            log.debug("Copied reference node {}", contentNodeRef.getId());


            try {
                Map<String, String[]> contentProperties = new HashMap<>(request.properties()) {{
                    put(CCConstants.CCM_PROP_IO_ORIGINAL, new String[]{originalNode.getId()});
                }};
                nodeService.updateNode(contentNodeRef.getId(), contentProperties, false);
                log.debug("Updated properties for new submission file {} with {}", nodeId, contentProperties);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            nodeService.addAspect(contentNodeRef.getId(), CCConstants.CCM_ASPECT_SUBMISSION_FILE_CONTENT);
            log.debug("Added content aspect to content node {}", contentNodeRef.getId());

            permissionService.setPermission(contentNodeRef.getId(), currentUser, CCConstants.PERMISSION_CONSUMER);
            log.debug("Added consumer permission for {} to submission file {}", currentUser, contentNodeRef.getId());
            return null;
        });
    }

    private void handleFileUpload(SubmissionFileRequest request, InputStream fileInputStream) {
        String currentUser = AuthenticationUtil.getRunAsUser();
        AuthenticationUtil.runAsSystem(() -> {
            String contentNodeId;
            try {
                Map<String, String[]> contentProperties = new HashMap<>(request.properties()) {{
                    put(CCConstants.CM_NAME, new String[]{"content"});
                    put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FILE, new String[]{request.assignmentFile()});
                }};
                contentNodeId = nodeService.createNode(nodeId, CCConstants.CCM_TYPE_IO, contentProperties, CCConstants.CCM_ASPECT_SUBMISSION_FILE_CONTENT, true, null);
                log.debug("Created new submission file content node {} for {}", contentNodeId, nodeId);

                apiClient.writeContent(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, contentNodeId, fileInputStream, null, null, CCConstants.CM_PROP_CONTENT);
                log.debug("Uploaded submission file content for {} to {}", nodeId, contentNodeId);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            nodeService.addAspect(contentNodeId, CCConstants.CCM_ASPECT_SUBMISSION_FILE_CONTENT);
            log.debug("Added content aspect to content node {}", contentNodeId);

            permissionService.setPermission(contentNodeId, currentUser, CCConstants.PERMISSION_CONSUMER);
            log.debug("Added consumer permission for {} to submission file {}", currentUser, contentNodeId);
            return null;
        });
    }

    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void setValidationStatus(Submission.Status validationStatus) {
        validateExists();
        submissionDao.validateCanCoordinatorChangeSubmission();

        refresh();

        nodeService.updateNodeNative(nodeId, Map.of(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, validationStatus.name()));
        refresh();
    }

    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void delete() {
        if (!exists()) {
            return;
        }

        submissionDao.validateAssigneeCanChangeSubmission();
        AuthenticationUtil.runAsSystem(() -> {
            doDelete();
            return null;
        });
        refresh();
    }

    @Override
    public SubmissionFile getSubmissionFile() {
        if (!exists()) {
            return null;
        }


        Node correctionNode = null;
        if (AssignmentUtil.isAssignmentCoordinator(permissionService, getNodeId()) || submissionDao.isReturned()) {
            correctionNode = this.correctionNode.get();
        }


        return new SubmissionFile(
                getNodeRef(),
                this.contentNode.get(),
                correctionNode,
                getReferToAssigmentFile().map(AssignmentFileDao::getAssignmentFile).orElse(null),
                getValidationStatus());
    }

    @Override
    public Submission.Status getValidationStatus() {
        if (AssignmentUtil.isAssignmentCoordinator(permissionService, getCorrectionNodeId())) {
            return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class);
        } else {
            return null;
        }
    }

    @Override
    public Optional<AssignmentFileDao> getReferToAssigmentFile() {
        NodeRef nodeRef = propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FILE);
        if (nodeRef == null) {
            return Optional.empty();
        }
        return Optional.of(assignmentDao.getAssignmentFile(nodeRef.getId()));
    }

    @Override
    public String getContentNodeId() {
        return contentNodeRef.get().map(NodeRef::getId)
                .orElseThrow(() -> new IllegalStateException("No content node found for submission file " + nodeId));
    }

    @Override
    public String getCorrectionNodeId() {
        return correctionNodeRef.get().map(NodeRef::getId)
                .orElse(null);
    }


    @Override
    public NodeRef getAlfrescoContentNodeRef() {
        return contentNodeRef.get()
                .orElseThrow(() -> new IllegalStateException("No content node found for submission file " + nodeId));
    }

    @Override
    public NodeRef getAlfrescoCorrectionNodeRef() {
        return correctionNodeRef.get()
                .orElse(null);
    }

    private String getContentNodeId_Internal() {
        return contentNodeRef.get().map(NodeRef::getId).orElse(null);
    }
}
