package org.edu_sharing.service.assignment.dao;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionValidationRequest;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.assignment.SubmissionDao;
import org.edu_sharing.service.assignment.SubmissionFileDao;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.util.LazyProvider;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
final class NodeSubmissionDao extends BasicNodeDaoImpl implements SubmissionDao {

    @Setter(onMethod_ = @Autowired)
    private PermissionService permissionService;
    @Setter(onMethod_ = @Autowired)
    private AuthorityService authorityService;
    @Setter(onMethod_ = @Autowired)
    private BehaviourFilter policyBehaviourFilter;

    private AssignmentDaoFactory assignmentDaoFactory;
    private NodeSubmissionAssignmentDao assignmentDao;

    private final LazyProvider<Map<String, SubmissionFileDao>> submissionFileRefs = new LazyProvider<>(() -> {
        validateExists();
        return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_SUBMISSION_FILE)
                .stream()
                .map(ChildAssociationRef::getChildRef)
                .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                .map(x -> assignmentDaoFactory.submissionFileDao(assignmentDao, this, x))
                .collect(Collectors.toMap(SubmissionFileDao::getNodeId, x -> x)));
    });

    public NodeSubmissionDao(AssignmentDaoFactory assignmentDaoFactory, NodeSubmissionAssignmentDao assignmentDao, String nodeId) {
        super(nodeId);
        this.assignmentDaoFactory = assignmentDaoFactory;
        this.assignmentDao = assignmentDao;
    }

    public NodeSubmissionDao(AssignmentDaoFactory assignmentDaoFactory, NodeSubmissionAssignmentDao assignmentDao, NodeRef nodeRef) {
        super(nodeRef);
        this.assignmentDaoFactory = assignmentDaoFactory;
        this.assignmentDao = assignmentDao;
    }


    @Override
    public Submission getSubmission() {
        if (!exists()) {
            return null;
        }

        String creator = getCreator();
        return new Submission(
                getNodeRef(),
                UserSimple.create(authorityService.getUser(creator), creator),
                getValidationNotes(),
                getFeedback(),
                getStatus(),
                getValidationStatus(),
                getSubmissionDate(),
                getReturnDate(),
                getUserNotes()
        );
    }

    @Override
    public Date getReturnDate() {
        return propertyMapper.get().getDate(CCConstants.CCM_PROP_SUBMISSION_RETURN_DATE);
    }

    @Override
    public Date getSubmissionDate() {
        return propertyMapper.get().getDate(CCConstants.CCM_PROP_SUBMISSION_SUBMISSION_DATE);
    }

    @Override
    public boolean isReturned() {
        return switch (assignmentDao.getStatus()) {
            case CORRECTED, FINISHED ->
                    propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class) == Submission.Status.FINISHED;
            default -> false;
        };
    }

    @Override
    @RetryingTransaction // node does not exists after return, because rollback is performed for no reason
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void updateValidationInfo(SubmissionValidationRequest request) {
        validateExists();
        refresh();
        validateCanCoordinatorChangeSubmission();

        AuthenticationUtil.runAsSystem(() -> {
            boolean statusChanged = false;
            Map<String, Object> properties = new HashMap<>();
            if (request.validationStatus() != null) {
                properties.put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, request.validationStatus().name());
                properties.put(CCConstants.CCM_PROP_SUBMISSION_RETURN_DATE, request.validationStatus() == Submission.Status.FINISHED ? new Date() : null);
                statusChanged = true;
            }

            if (request.validationNotes() != null) {
                properties.put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_NOTES, request.validationNotes());
            }

            if (request.feedback() != null) {
                properties.put(CCConstants.CCM_PROP_SUBMISSION_FEEDBACK, request.feedback());
            }

            nodeService.updateNodeNative(nodeId, properties);
            refresh();

            // we also have to set status of submissionfiles
            if (statusChanged) {
                getSubmissionFiles()
                        .stream()
                        .map(SubmissionFileDao::getCorrectionNodeId).filter(Objects::nonNull)
                        .forEach(x -> {
                            try {
                                if (getValidationStatus() == Submission.Status.FINISHED) {
                                    permissionService.setPermission(x, getCreator(), CCConstants.PERMISSION_CONSUMER);
                                } else {
                                    permissionService.removePermission(x, getCreator(), CCConstants.PERMISSION_CONSUMER);
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        });
            }
            return null;
        });

    }


    @Override
    public void setStatus(Submission.Status status) {
        validateExists();
        refresh();

        Submission.Status currentStatus = getStatus();
        if (currentStatus == status) {
            log.debug("Submission status of {}, is already set to {}", nodeId, status);
            return;
        }

        validateAssigneeCanChangeSubmission();
        validateAssignmentCoordinatorCanChangeState(status);

        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CCM_PROP_SUBMISSION_STATUS, status.name());
            put(CCConstants.CCM_PROP_SUBMISSION_SUBMISSION_DATE, status == Submission.Status.FINISHED ? new Date() : null);
        }};

        AuthenticationUtil.runAsSystem(() -> {
            nodeService.updateNodeNative(nodeId, properties);
            return null;
        });
        refresh();
    }

    @Override
    public List<SubmissionFileDao> getSubmissionFiles() {
        return submissionFileRefs.get().values().stream().toList();
    }

    @Override
    public SubmissionFileDao getSubmissionFile(String submissionFileId) {
        SubmissionFileDao submissionFileDao = submissionFileRefs.get().get(submissionFileId);
        if (submissionFileDao == null) {
            throw new IllegalArgumentException("Submission file with id " + submissionFileId + " does not exist.");
        }
        return submissionFileDao;
    }

    @Override
    @RetryingTransaction // node does not exists after return, because rollback is performed for no reason
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public SubmissionFileDao createSubmissionFile(SubmissionFileRequest submissionFileRequest, InputStream fileInputStream, FormDataContentDisposition fileMetaData) {
        submissionFileRefs.invalidate();
        validateAssigneeCanChangeSubmission();

        SubmissionFileDao submissionFileDao = assignmentDaoFactory.submissionFileDao(assignmentDao, this, null);
        submissionFileDao.create(submissionFileRequest, fileInputStream);
        submissionFileRefs.get().put(submissionFileDao.getNodeId(), submissionFileDao);

        return submissionFileDao;
    }

    @Override
    @RetryingTransaction // node does not exists after return, because rollback is performed for no reason
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void create() {
        if (StringUtils.isNotBlank(nodeId)) {
            throw new IllegalStateException("Submission with id " + getNodeId() + " already exists.");
        }

        validateAssigneeCanChangeSubmission();

        log.debug("Creating new submission");
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CCM_PROP_SUBMISSION_STATUS, Submission.Status.NOT_STARTED.name());
            put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.NOT_STARTED.name());
        }};

        String user = AuthenticationUtil.getRunAsUser();

        AuthenticationUtil.runAsSystem(() -> {
            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, assignmentDao.getSubmissionRefId(), CCConstants.CCM_TYPE_SUBMISSION, CCConstants.CCM_ASSOC_SUBMISSIONS_SUBMISSION, properties);
            // if a coordinator creates a submission, the creator and modifier needs to be set to the run as user otherwise it will be set to the coordinator
            policyBehaviourFilter.disableBehaviour();
            try {
                nodeService.setProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CM_PROP_C_CREATOR, user, true);
                nodeService.setProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CM_PROP_C_MODIFIER, user, true);
            } finally {
                policyBehaviourFilter.enableBehaviour();
            }
            nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
            log.debug("Created new submission for {}({}) to {}", user, nodeId, assignmentDao.getSubmissionRefId());

            permissionService.setPermission(nodeId, user, CCConstants.PERMISSION_ASSIGNEE);
            permissionService.setPermission(nodeId, user, CCConstants.PERMISSION_COMMENT);

            log.debug("Added permission {} for {} to submission {}", CCConstants.PERMISSION_ASSIGNEE, user, nodeId);
            return null;
        });
    }


    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void delete() {
        if (!exists()) {
            return;
        }
        validateAssigneeCanChangeSubmission();
        doDelete();
        refresh();
    }

    @Override
    public Submission.Status getStatus() {
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_STATUS, Submission.Status.class);
    }

    @Override
    public Submission.Status getValidationStatus() {
        if (isReturned()) {
            return Submission.Status.FINISHED;
        }

        if (!AssignmentUtil.isAssignmentCoordinator(permissionService, nodeId) && !isReturned()) {
            return Submission.Status.PENDING;
        }
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class);
    }

    @Override
    public String getFeedback() {
        if (!AssignmentUtil.isAssignmentCoordinator(permissionService, nodeId)
                && !isReturned()
                && propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class) != Submission.Status.FINISHED) {
            return null;
        }

        return propertyMapper.get().getString(CCConstants.CCM_PROP_SUBMISSION_FEEDBACK);
    }

    @Override
    public String getValidationNotes() {
        if (!AssignmentUtil.isAssignmentCoordinator(permissionService, nodeId)) {
            return null;
        }
        return propertyMapper.get().getString(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_NOTES);
    }

    @Override
    public String getUserNotes() {
        return propertyMapper.get().getString(CCConstants.CCM_PROP_SUBMISSION_USER_NOTES);
    }

    @Override
    @RetryingTransaction
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNEE)")
    public void setUserNotes(String userNotes) {
        validateExists();
        validateAssigneeCanChangeSubmission();
        AuthenticationUtil.runAsSystem(() -> {
            nodeService.updateNodeNative(nodeId, Map.of(CCConstants.CCM_PROP_SUBMISSION_USER_NOTES, userNotes));
            return null;
        });
        refresh();
    }

    void validateCanCoordinatorChangeSubmission() {
        if (AssignmentUtil.isAssignmentCoordinator(permissionService, nodeId)) {
            return;
        }

        if (getStatus() == Submission.Status.FINISHED) {
            return;
        }

        if (assignmentDao.getEndDate() != null && assignmentDao.getEndDate().before(new Date())) {
            throw new InsufficientPermissionException("Assignment with id " + assignmentDao.getNodeId() + " has not yet been completed.");
        }
    }

    void validateAssigneeCanChangeSubmission() {
        if (AssignmentUtil.isAssignmentCoordinator(permissionService, assignmentDao.getNodeId())) {
            return;
        }

        if (assignmentDao.getStatus() == Assignment.Status.DRAFT) {
            throw new InsufficientPermissionException("Assignment with id " + assignmentDao.getNodeId() + " is in draft mode and cannot be modified by assignees.");
        }

        if (assignmentDao.getEndDate() != null && assignmentDao.getEndDate().before(new Date())) {
            throw new InsufficientPermissionException("Assignment with id " + assignmentDao.getNodeId() + " has already ended.");
        }

        if (!exists()) {
            return;
        }

        if (getStatus() == Submission.Status.FINISHED || isReturned()) {
            throw new InsufficientPermissionException("Submission with id " + getNodeId() + " has already been finished.");
        }
    }


    private void validateAssignmentCoordinatorCanChangeState(Submission.Status newStatus) {
        if (!AssignmentUtil.isAssignmentCoordinator(permissionService, nodeId)) {
            return;
        }

        if (getStatus() != Submission.Status.FINISHED && newStatus != Submission.Status.PENDING) {
            throw new InsufficientPermissionException("Submission status can only be changed from FINISHED to PENDING by assignment coordinator");
        }
    }
}
