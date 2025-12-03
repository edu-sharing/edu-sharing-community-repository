package org.edu_sharing.service.assignment.dao;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFile;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.BasicNodeDao;
import org.edu_sharing.service.assignment.SubmissionFileDao;
import org.edu_sharing.service.permission.PermissionService;
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
final class SubmissionFileDaoImpl extends BasicNodeDaoImpl implements SubmissionFileDao {

    private final AssignmentDaoImpl assignmentDao;
    private final SubmissionDaoImpl submissionDao;

    private final LazyProvider<Optional<NodeRef>> contentNodeId;
    private final LazyProvider<Node> contentNode;

    @Setter(onMethod_ = @Autowired)
    private PermissionService permissionService;

    @Setter(onMethod_ = @Autowired)
    private ContentService contentService;


    public SubmissionFileDaoImpl(AssignmentDaoImpl assignmentDao, SubmissionDaoImpl submissionDao, String nodeId) {
        super(nodeId);
        this.assignmentDao = assignmentDao;
        this.submissionDao = submissionDao;

        contentNodeId = new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_ASSOC_SUBMISSION_FILE_DATA)
                    .stream()
                    .map(ChildAssociationRef::getChildRef)
                    .findFirst());
        });

        contentNode = new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            return contentNodeId.get()
                    .map(NodeDao::getAsNodeSimple)
                    .orElse(null);
        }));
    }

    @Override
    public void refresh() {
        propertyMapper.invalidate();
        contentNodeId.invalidate();
        contentNode.invalidate();
    }


    @Override
//        @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void create(SubmissionFileRequest request, InputStream fileInputStream) {
        // TODO Check if a submission file to the same submission file already exists
        submissionDao.validateAssigneeCanChangeSubmission();

        if (submissionDao.getSubmissionFiles()
                .stream()
                .anyMatch(x -> x.getReferToAssigmentFile()
                        .map(BasicNodeDao::getNodeId)
                        .map(y -> y.equals(request.assignmentFile()))
                        .orElse(false))) {
            throw new IllegalStateException("A submission file to the same assignment file already exists.");
        }

        if (StringUtils.isNotBlank(nodeId)) {
            throw new IllegalStateException("Submission file with id " + nodeId + " already exists.");
        }

        // validates if assignment file exists otherwise throws exception
        assignmentDao.getAssignmentFile(request.assignmentFile());

        log.debug("Creating new submission file");
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.assignmentFile()));
        }};

        AuthenticationUtil.runAsSystem(() -> {
            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, submissionDao.getNodeId(), CCConstants.CCM_TYPE_SUBMISSION_FILE, CCConstants.CCM_ASSOC_SUBMISSION_FILES, properties);
            nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
            return null;
        });

        log.debug("Created new submission file for {} to {}", nodeId, submissionDao.getNodeId());
        handleSubmissionFile(request, fileInputStream);
    }

    @Override
//        @RetryingTransaction // node does not exists after return, because rollback is performed for no reason
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void update(SubmissionFileRequest request, InputStream fileInputStream) {
        refresh();
        validateExists();

        submissionDao.validateAssigneeCanChangeSubmission();

        if (submissionDao.getSubmissionFiles()
                .stream()
                .anyMatch(x -> !x.getNodeId().equals(getNodeId()) && x.getReferToAssigmentFile()
                        .map(BasicNodeDao::getNodeId)
                        .map(y -> y.equals(request.assignmentFile()))
                        .orElse(false))) {
            throw new IllegalStateException("A submission file to the same assignment file already exists.");
        }

        // validates if assignment file exists otherwise throws exception
        assignmentDao.getAssignmentFile(request.assignmentFile());

        AuthenticationUtil.runAsSystem(() -> {

            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.assignmentFile()));
            }};

            nodeService.updateNodeNative(nodeId, properties);
            handleSubmissionFile(request, fileInputStream);
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
        if (Boolean.parseBoolean(nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), request.originalFile(), CCConstants.CCM_PROP_RESTRICTED_ACCESS))) {
            log.debug("Skipping reference copy for restricted access document");
            return;
        }

        if (!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), request.originalFile(), CCConstants.PERMISSION_DOWNLOAD_CONTENT)) {
            throw new InsufficientPermissionException("You do not have permission to copy the original file. Required permission: " + CCConstants.PERMISSION_DOWNLOAD_CONTENT);
        }

        AuthenticationUtil.runAsSystem(() -> {
            log.debug("Copying reference node {}", request.originalFile());
            NodeRef contentNodeRef = nodeService.copyNode(request.originalFile(), nodeId, CCConstants.CCM_ASSOC_SUBMISSION_FILE_DATA, true);
            nodeService.setOwner(contentNodeRef.getId(), ApplicationInfoList.getHomeRepository().getUsername());
            log.debug("Copied reference node {}", contentNodeRef.getId());


            try {
                Map<String, String[]> contentProperties = new HashMap<>(request.properties()) {{
                    put(CCConstants.CCM_PROP_IO_ORIGINAL, new String[]{request.originalFile()});
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

            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            permissionService.setPermission(contentNodeRef.getId(), currentUser, CCConstants.PERMISSION_CONSUMER);
            log.debug("Added consumer permission for {} to submission file {}", currentUser, contentNodeRef.getId());
            return null;
        });
    }

    private void handleFileUpload(SubmissionFileRequest request, InputStream fileInputStream) {
        AuthenticationUtil.runAsSystem(() -> {
            String contentNodeId;
            try {
                Map<String, String[]> contentProperties = new HashMap<>(request.properties()) {{
                    put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new String[]{request.assignmentFile()});
                }};
                contentNodeId = nodeService.createNode(nodeId, CCConstants.CCM_TYPE_SUBMISSION_FILE, contentProperties, CCConstants.CCM_ASSOC_SUBMISSION_FILES, true);
                log.debug("Created new submission file content node {} for {}", contentNodeId, nodeId);

                ContentWriter writer = contentService.getWriter(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), ContentModel.PROP_CONTENT, true);
                writer.putContent(fileInputStream);
                log.debug("Uploaded submission file content for {} to {}", nodeId, contentNodeId);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            nodeService.addAspect(contentNodeId, CCConstants.CCM_ASPECT_SUBMISSION_FILE_CONTENT);
            log.debug("Added content aspect to content node {}", contentNodeId);

            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            permissionService.setPermission(contentNodeId, AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.PERMISSION_CONSUMER);
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
        validateExists();
        submissionDao.validateAssigneeCanChangeSubmission();
    }

    @Override
    public SubmissionFile getSubmissionFile() {
        if (!exists()) {
            return null;
        }

        return new SubmissionFile(
                getNodeRef(),
                contentNode.get(),
                getReferToAssigmentFile().map(AssignmentFileDao::getAssignmentFile).orElse(null),
                getValidationStatus());
    }

    @Override
    public Submission.Status getValidationStatus() {
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class);
    }

    @Override
    public Optional<AssignmentFileDao> getReferToAssigmentFile() {
        NodeRef nodeRef = propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE);
        if (nodeRef == null) {
            return Optional.empty();
        }
        return Optional.of(assignmentDao.getAssignmentFile(nodeRef.getId()));
    }

    @Override
    public String getContentNodeId() {
        return contentNodeId.get().map(NodeRef::getId)
                .orElseThrow(() -> new IllegalStateException("No content node found for submission file " + nodeId));
    }

    private String getContentNodeId_Internal() {
        return contentNodeId.get().map(NodeRef::getId).orElse(null);
    }
}
