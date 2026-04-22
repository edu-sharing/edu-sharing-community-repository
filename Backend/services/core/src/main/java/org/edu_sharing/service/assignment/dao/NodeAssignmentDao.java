package org.edu_sharing.service.assignment.dao;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;
import org.edu_sharing.restservices.assignment.v1.model.CreateAssignmentRequest;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.assignment.AssignmentConfig;
import org.edu_sharing.service.assignment.AssignmentDao;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.SubmissionDao;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.util.CheckedRunAsWork;
import org.edu_sharing.util.LazyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
final class NodeAssignmentDao extends BasicNodeDaoImpl implements AssignmentDao {


    @Setter(onMethod_ = @Autowired)
    private AssignmentDaoFactory assignmentDaoFactory;
    @Setter(onMethod_ = @Autowired)
    private PermissionService permissionService;
    @Setter(onMethod_ = @Autowired)
    private UserEnvironmentTool userEnvironmentTool;
    @Setter(onMethod_ = @Autowired)
    private AssignmentConfig assignmentConfig;
    @Setter(onMethod_ = @Autowired)
    private AuthorityService authorityService;

    private final LazyProvider<Map<String, AssignmentFileDao>> assignmentFileRefs = registerLazyProvider(new LazyProvider<>(() -> {
        validateExists();
        return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE)
                .stream()
                .map(ChildAssociationRef::getChildRef)
                .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                .map(x -> assignmentDaoFactory.assignmentFileDao(this, x))
                .collect(Collectors.toMap(AssignmentFileDao::getNodeId, x -> x)));
    }));


    private final LazyProvider<List<Assignment.Permission>> permissions;

    public NodeAssignmentDao() {
        this((String)null);
    }

    public NodeAssignmentDao(String nodeId) {
        super(nodeId);

        permissions = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            if (!AssignmentUtil.isAssignmentCoordinator(permissionService, getNodeId())) {
                return Collections.emptyList();
            }

            return AuthenticationUtil.runAsSystem(CheckedRunAsWork.wrap(() ->
                    Arrays.stream(permissionService.getPermissions(getNodeId()).getAces())
                            .map(ace -> {
                                String permission = ace.getPermission();
                                Assignment.Role role = mapPermissionToRole(permission);
                                if (role == null) {
                                    log.error("Unknown permission for assignment {} {}", nodeId, permission);
                                    return null;
                                }
                                return new Assignment.Permission(new Authority(ace), role);
                            })
                            .filter(Objects::nonNull)
                            .toList()
            ));
        }));
    }

    public NodeAssignmentDao(org.edu_sharing.service.model.NodeRef nodeRef) {
        super(nodeRef);
        permissions = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            if (!AssignmentUtil.isAssignmentCoordinator(permissionService, getNodeId())) {
                return Collections.emptyList();
            }

            String user = AuthenticationUtil.getRunAsUser();
            return nodeRef.getPermissions()
                    .entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .map(y -> {
                        Assignment.Role role = mapPermissionToRole(y);
                        if (role == null) {
                            return null;
                        }

                        return new Assignment.Permission(new Authority(user, y), role);
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }));
    }

    private Assignment.Role mapPermissionToRole(String permission) {
        return switch (permission) {
            // filter consumer role
            case CCConstants.PERMISSION_CONSUMER -> null;
            case CCConstants.PERMISSION_ASSIGNEE -> {
                log.debug(CCConstants.PERMISSION_ASSIGNEE + " should not be set for assignment {} of type {}", getNodeId(), Assignment.Type.DEFAULT);
                yield null;
            }
            case CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR -> Assignment.Role.COORDINATOR;
            default -> null;
        };
    }

    @Override
    @RunAsSystem
    @RetryingTransaction
    @Permission(value = CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_ASSIGNMENTS, requiresUser = true)
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void createOrUpdate(CreateAssignmentRequest request) {
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CM_PROP_TITLE, request.title());
            put(CCConstants.CM_PROP_DESCRIPTION, request.summary());
            put(CCConstants.CCM_PROP_ASSIGNMENT_TYPE, Assignment.Type.DEFAULT);
            put(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, request.status().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, request.allowAdditionalDocumentSubmissions());
            put(CCConstants.CCM_PROP_ASSIGNMENT_END_DATE, request.endTime());
        }};

        if (StringUtils.isNotBlank(nodeId)) {
            validateExists();
            log.debug("Update assignment node {} with {}", nodeId, properties);
            nodeService.updateNodeNative(nodeId, properties);
        } else {
            try {
                String assignmentFolder = userEnvironmentTool.getEdu_SharingAssignmentFolder();
                String parentFolder = NodeServiceHelper.getContainerId(assignmentFolder, assignmentConfig.getNodePattern());
                nodeId = nodeService.createNodeBasic(parentFolder, CCConstants.CCM_TYPE_ASSIGNMENT, properties);
                nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
                log.debug("Created assignment node {}", nodeId);
            } catch (Throwable t) {
                log.error("Error while creating assignment", t);
                throw new RuntimeException(t);
            }
        }


        try {
            List<ACE> aceList = new ArrayList<>(request.permissions()
                    .stream()
                    .flatMap(x -> switch (x.role()) {
                        case ASSIGNEE ->
                                throw new IllegalArgumentException(Assignment.Role.ASSIGNEE + " cannot be set for assignment of type " + Assignment.Type.DEFAULT);
                        case COORDINATOR ->
                                Stream.of(new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, x.authorityName()));
                    })
                    .toList());
            aceList.add(new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, AuthenticationUtil.getRunAsUser()));
            log.debug("Setting permissions for assignment {}: {}", nodeId, aceList);
            permissionService.setPermissions(nodeId, aceList, false);
        } catch (Exception t) {
            throw new RuntimeException(t);
        }

        updateAssignmentFiles(request);
        refresh();
    }

    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void delete() {
        if (!exists()) {
            return;
        }
        doDelete();
        refresh();
    }

    private void updateAssignmentFiles(CreateAssignmentRequest request) {
        log.debug("Updating assignment files for assignment {}", nodeId);
        // add/update/delete assignment files
        Map<String, AssignmentFileDao> assignmentFileDaoMap = getAssignmentFiles()
                .stream()
                .collect(Collectors.toMap(AssignmentFileDao::getReferNodeId, x -> x));

        // delete assignment files that are not in the request
        Map<String, AssignmentFileRequest> newAssignmentFileRequestMap = request.assignmentFiles()
                .stream()
                .collect(Collectors.toMap(AssignmentFileRequest::refId, x -> x));
        HashMap<String, AssignmentFileDao> assignmentFileDaoToDelete = new HashMap<>(assignmentFileDaoMap);
        assignmentFileDaoToDelete.keySet().removeAll(newAssignmentFileRequestMap.keySet());
        log.debug("Deleted assignment files: {}", assignmentFileDaoToDelete.keySet());
        assignmentFileDaoToDelete.values().forEach(AssignmentFileDao::delete);

        // update assignment files that are in the request
        assignmentFileDaoMap.keySet().retainAll(newAssignmentFileRequestMap.keySet());
        log.debug("Updated assignment files: {}", assignmentFileDaoMap.keySet());
        assignmentFileDaoMap.forEach((refId, dao) -> dao.update(newAssignmentFileRequestMap.get(refId)));

        // add new assignment files
        newAssignmentFileRequestMap.keySet().removeAll(assignmentFileDaoMap.keySet());
        log.debug("Added assignment files: {}", newAssignmentFileRequestMap.keySet());
        newAssignmentFileRequestMap.values().forEach(x -> assignmentDaoFactory.assignmentFileDao(this, null).create(x));
    }

    @Override
    public Assignment getAssignment() {
        if (!exists()) {
            return null;
        }

        String creator = getCreator();
        return new Assignment(
                getNodeRef(),
                getTitle(),
                getSummary(),
                UserSimple.create(authorityService.getUser(creator), creator),
                getCreateDate(),
                getEndDate(),
                null,
                getStatus(),
                getType(),
                getAllowAdditionalDocumentSubmissions(),
                getModifiedDate(),
                getPermissions(),
                // TODO filter by permission
                Collections.emptyList()
        );
    }

    @Override
    public Boolean getAllowAdditionalDocumentSubmissions() {
        return propertyMapper.get().getBoolean(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, false);
    }

    @Override
    public Assignment.Type getType() {
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_ASSIGNMENT_TYPE, Assignment.Type.class);
    }

    @Override
    public Assignment.Status getStatus() {
        return propertyMapper.get().getEnum(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, Assignment.Status.class);
    }

    @Override
    public Date getEndDate() {
        return propertyMapper.get().getDate(CCConstants.CCM_PROP_ASSIGNMENT_END_DATE);
    }

    @Override
    public String getSummary() {
        return propertyMapper.get().getString(CCConstants.CM_PROP_DESCRIPTION);
    }

    @Override
    public String getTitle() {
        return propertyMapper.get().getString(CCConstants.CM_PROP_TITLE);
    }


    @Override
    public Collection<AssignmentFileDao> getAssignmentFiles() {
        return assignmentFileRefs.get().values();
    }

    @Override
    public Collection<SubmissionDao> getSubmissions() {
        throw new UnsupportedOperationException("Submissions are not supported for assignment of type " + getType());
    }

    @Override
    public SubmissionDao getSubmission(String submissionId) {
        throw new UnsupportedOperationException("Submissions are not supported for assignment of type " + getType());
    }

    @Override
    public SubmissionDao getOrCreateSubmission(String submissionId) {
        throw new UnsupportedOperationException("Submissions are not supported for assignment of type " + getType());
    }


    @Override
    public List<Assignment.Permission> getPermissions() {
        return permissions.get();
    }

    @Override
    public String getSubmissionRefId() {
        throw new UnsupportedOperationException("Submissions are not supported for assignment of type " + getType());
    }

    @Override
    public AssignmentFileDao getAssignmentFile(String id) {
        AssignmentFileDao assignmentFileDao = assignmentFileRefs.get().get(id);
        if (assignmentFileDao == null) {
            throw new IllegalArgumentException("AssignmentFile with id " + id + " does not exist.");
        }
        return assignmentFileDao;
    }

    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void setStatus(Assignment.Status status) {
        validateExists();
        refresh();

        Assignment.Status currentStatus = getStatus();
        if (currentStatus == status) {
            log.debug("Submission status of {}, is already set to {}", nodeId, status);
            return;
        }

        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, status.name());
        }};

        AuthenticationUtil.runAsSystem(() -> {
            nodeService.updateNodeNative(nodeId, properties);
            return null;
        });
        refresh();
    }

    @Override
    public SubmissionDao createSubmissionByUserId(String username) {
        throw new UnsupportedOperationException("Submissions are not supported for assignment of type " + getType());
    }
}
