package org.edu_sharing.service.assignment.dao;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.MissingResourceException;
import org.edu_sharing.restservices.assignment.v1.model.*;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.assignment.AssignmentConfig;
import org.edu_sharing.service.assignment.AssignmentDao;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.SubmissionDao;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.util.CheckedRunAsWork;
import org.edu_sharing.util.LazyProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NodeSubmissionAssignmentDao extends BasicNodeDaoImpl implements AssignmentDao {

    private final LazyProvider<Map<String, AssignmentFileDao>> assignmentFileRefs;
    private final LazyProvider<List<Assignment.Permission>> permissions;
    private final LazyProvider<String> submissionFolderRef;
    private final LazyProvider<Map<String, SubmissionDao>> submissionsMap;
    private final LazyProvider<Collection<SubmissionDao>> submissions;

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


    public NodeSubmissionAssignmentDao() {
        this((String) null);
    }

    public NodeSubmissionAssignmentDao(String nodeId) {
        super(nodeId);
        assignmentFileRefs = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE)
                    .stream()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                    .map(x -> assignmentDaoFactory.submissionAssignmentFileDao(this, x))
                    .collect(Collectors.toMap(AssignmentFileDao::getNodeId, x -> x)));
        }));

        permissions = registerLazyProvider(createLazyPermissionProvider(nodeId));

        submissionFolderRef = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_SUBMISSIONS)
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getChildRef()
                    .getId());
        }));

        submissionsMap = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();

            List<ChildAssociationRef> childAssociationRefs = AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(submissionFolderRef.get(), CCConstants.CCM_TYPE_SUBMISSION));

            return childAssociationRefs.stream()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                    .filter(id -> AssignmentUtil.hasAccessTo(permissionService, id))
                    .collect(Collectors.toMap(x -> x, x -> assignmentDaoFactory.submissionDaoByNodeId(this, x)));
        }));

        submissions = registerLazyProvider(createLazySubmissionsProvider());

    }


    public NodeSubmissionAssignmentDao(org.edu_sharing.service.model.NodeRef nodeRef) {
        super(nodeRef);
        assignmentFileRefs = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE)
                    .stream()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                    .map(x -> assignmentDaoFactory.submissionAssignmentFileDao(this, x))
                    .collect(Collectors.toMap(AssignmentFileDao::getNodeId, x -> x)));
        }));

        permissions = registerLazyProvider(createLazyPermissionProvider(nodeRef.getNodeId()));

        submissionFolderRef = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();
            return nodeRef.getChildren()
                    .stream()
                    .filter(y -> CCConstants.CCM_TYPE_SUBMISSIONS.equals(y.getType()))
                    .findFirst()
                    .map(NodeRef::getNodeId)
                    .orElse(null);
        }));

        submissionsMap = registerLazyProvider(new LazyProvider<>(() -> {
            validateExists();

            return nodeRef.getChildren()
                    .stream()
                    .filter(y -> CCConstants.CCM_TYPE_SUBMISSIONS.equals(y.getType()))
                    .findFirst()
                    .map(x -> x.getChildren().stream()
                            .filter(AssignmentUtil::hasAccessTo)
                            .collect(Collectors.toMap(NodeRef::getNodeId, y -> assignmentDaoFactory.submissionDaoByNodeRef(this, y))))
                    .orElse(Collections.emptyMap());
        }));

        submissions = registerLazyProvider(createLazySubmissionsProvider());

    }


    @NotNull
    private LazyProvider<List<Assignment.Permission>> createLazyPermissionProvider(String nodeId) {
        return new LazyProvider<>(() -> {
            validateExists();
            if (!AssignmentUtil.isAssignmentCoordinator(permissionService, getNodeId())) {
                return Collections.emptyList();
            }

            return AuthenticationUtil.runAsSystem(CheckedRunAsWork.wrap(() ->
                    Arrays.stream(permissionService.getPermissions(getNodeId()).getAces())
                            .map(ace -> {
                                Assignment.Role role = mapPermissionToRole(ace.getPermission());
                                if (role == null) {
                                    log.error("Unknown permission for assignment {} {}", nodeId, ace.getPermission());
                                    return null;
                                }

                                return new Assignment.Permission(new Authority(ace), role);
                            })
                            .filter(Objects::nonNull)
                            .toList()
            ));
        });
    }

    @NotNull
    private LazyProvider<Collection<SubmissionDao>> createLazySubmissionsProvider() {
        return new LazyProvider<>(() -> {
            Collection<SubmissionDao> realSubmissions = submissionsMap.get().values();
            List<Assignment.Permission> permissions = getPermissions();

            Set<String> possibleAssignees = permissions.stream()
                    .filter(x -> x.role() == Assignment.Role.ASSIGNEE)
                    .map(x -> x.authority().getAuthorityName())
                    .collect(Collectors.toSet());

            possibleAssignees.removeAll(realSubmissions.stream().map(SubmissionDao::getCreator).collect(Collectors.toSet()));

            return Stream.concat(
                    realSubmissions.stream(),
                    possibleAssignees.stream().map(x -> assignmentDaoFactory.emptySubmissionDao(x))
            ).toList();
        });
    }

    private static Assignment.Role mapPermissionToRole(String permission) {
        return switch (permission) {
            // filter consumer role
            case CCConstants.PERMISSION_CONSUMER -> null;
            case CCConstants.PERMISSION_ASSIGNEE -> Assignment.Role.ASSIGNEE;
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
        if (request.type() != Assignment.Type.SUBMISSION) {
            throw new IllegalArgumentException("Cannot change assignment type from " + Assignment.Type.SUBMISSION + " to " + request.type());
        }

        if (request.status() != Assignment.Status.DRAFT) {
            if (request.permissions().stream().noneMatch(x -> x.role() == Assignment.Role.ASSIGNEE)) {
                throw new IllegalArgumentException("Permissions must contain at least one assignee");
            }
        }

        if (StringUtils.isNotBlank(nodeId)) {
            update(request);
        } else {
            create(request);
        }
    }

    private void create(CreateAssignmentRequest request) {
        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CM_PROP_TITLE, request.title());
            put(CCConstants.CM_PROP_DESCRIPTION, request.summary());
            put(CCConstants.CCM_PROP_ASSIGNMENT_TYPE, Assignment.Type.SUBMISSION.name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, request.status().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, request.allowAdditionalDocumentSubmissions());
            put(CCConstants.CCM_PROP_ASSIGNMENT_END_DATE, request.endTime());
        }};

        String assignmentFolder = userEnvironmentTool.getEdu_SharingAssignmentFolder();
        String parentFolder = NodeServiceHelper.getContainerId(assignmentFolder, assignmentConfig.getNodePattern());
        nodeId = nodeService.createNodeBasic(parentFolder, CCConstants.CCM_TYPE_ASSIGNMENT, properties);
        nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
        log.debug("Created assignment node {}", nodeId);

        // subfolder for submissions + permission only for type SUBMISSION
        Map<String, Object> submissionsProperties = Map.of(
                CCConstants.CM_NAME, "submissions",// NodeRef
                CCConstants.CM_PROP_TITLE, "Submissions"
        );
        String submissionId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CCM_TYPE_SUBMISSIONS, CCConstants.CCM_ASSOC_ASSIGNMENT_SUBMISSIONS, submissionsProperties);
        nodeService.setOwner(submissionId, ApplicationInfoList.getHomeRepository().getUsername());
        log.debug("Created submissions node {}", submissionId);

        setAssignmentPermissions(request.permissions());
        setSubmissionPermissions(request.permissions());
        updateAssignmentFiles(request.assignmentFiles());
    }

    private void update(CreateAssignmentRequest request) {
        validateExists();

        Map<String, Object> properties = new HashMap<>() {{
            put(CCConstants.CM_NAME, UUID.randomUUID().toString());
            put(CCConstants.CM_PROP_TITLE, request.title());
            put(CCConstants.CM_PROP_DESCRIPTION, request.summary());
            put(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, request.status().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, request.allowAdditionalDocumentSubmissions());
            put(CCConstants.CCM_PROP_ASSIGNMENT_END_DATE, request.endTime());
        }};

        if (!canChangeMetadata()) {
            throw new IllegalStateException("Cannot change metadata of assignment " + nodeId);
        }

        log.debug("Update assignment node {} with {}", nodeId, properties);
        nodeService.updateNodeNative(nodeId, properties);

        setAssignmentPermissions(request.permissions());
        setSubmissionPermissions(request.permissions());
        updateAssignmentFiles(request.assignmentFiles());
    }


    private void setAssignmentPermissions(List<PermissionRequest> permissions) {
        List<ACE> aceList = new ArrayList<>(permissions
                .stream()
                .flatMap(x -> switch (x.role()) {
                    case ASSIGNEE -> {
                        AuthorityType authorityType = AuthorityType.getAuthorityType(x.authorityName());
                        if (authorityType == AuthorityType.GROUP) {
                            yield authorityService.getMembershipsOfGroupRecursively(x.authorityName())
                                    .stream()
                                    .flatMap(y -> Stream.of(new ACE(CCConstants.PERMISSION_ASSIGNEE, y), new ACE(CCConstants.PERMISSION_CONSUMER, y)));

                        } else {
                            yield Stream.of(new ACE(CCConstants.PERMISSION_ASSIGNEE, x.authorityName()), new ACE(CCConstants.PERMISSION_CONSUMER, x.authorityName()));
                        }
                    }
                    case COORDINATOR ->
                            Stream.of(new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, x.authorityName()));
                })
                .toList());
        aceList.add(new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, AuthenticationUtil.getFullyAuthenticatedUser()));
        log.debug("Setting permissions for assignment {}: {}", nodeId, aceList);
        permissionService.setPermissions(nodeId, aceList, false);
        refresh();
    }

    private void setSubmissionPermissions(List<PermissionRequest> permissions) {
        List<ACE> aceList = new ArrayList<>(permissions
                .stream()
                .filter(x -> x.role() != Assignment.Role.ASSIGNEE)
                .map(x ->
                        switch (x.role()) {
                            case ASSIGNEE ->
                                    throw new NotImplementedException("Something went wrong, assignee should not be in the permission list");
                            case COORDINATOR ->
                                    new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, x.authorityName());
                        }
                )
                .toList());
        aceList.add(new ACE(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, AuthenticationUtil.getFullyAuthenticatedUser()));
        log.debug("Setting permissions for submissions folder {}: {}", submissionFolderRef.get(), aceList);
        permissionService.setPermissions(submissionFolderRef.get(), aceList, false);
        refresh();
    }

    private void updateAssignmentFiles(List<AssignmentFileRequest> assignmentFiles) {
        log.debug("Updating assignment files for assignment {}", nodeId);
        // add/update/delete assignment files
        Map<String, AssignmentFileDao> existingAssignmentFileDaoMap = getAssignmentFiles()
                .stream()
                .collect(Collectors.toMap(AssignmentFileDao::getReferNodeId, x -> x));

        Map<String, AssignmentFileRequest> newAssignmentFileRequestMap = assignmentFiles
                .stream()
                .collect(Collectors.toMap(AssignmentFileRequest::refId, x -> x));
        // keep initial data to reuse later for update
        Map<String, AssignmentFileRequest> newAssignmentFileRequestMapInitial = new HashMap<>(newAssignmentFileRequestMap);

        // delete assignment files that are not in the request
        Map<String, AssignmentFileDao> assignmentFileDaoToDelete = new HashMap<>(existingAssignmentFileDaoMap);
        assignmentFileDaoToDelete.keySet().removeAll(newAssignmentFileRequestMap.keySet());

        // update assignment files that are in the request
        existingAssignmentFileDaoMap.keySet().retainAll(newAssignmentFileRequestMap.keySet());

        // add new assignment files
        newAssignmentFileRequestMap.keySet().removeAll(existingAssignmentFileDaoMap.keySet());

        if ((!assignmentFileDaoToDelete.isEmpty() || !newAssignmentFileRequestMap.isEmpty())) {
            if (!canChangeAssignment()) {
                throw new IllegalStateException("Cannot delete assignment files for assignment " + nodeId + " because it is not editable state");
            }
        }

        log.debug("Deleted assignment files: {}", assignmentFileDaoToDelete.keySet());
        assignmentFileDaoToDelete.values().forEach(AssignmentFileDao::delete);

        if (canChangeAssignment()) {
            log.debug("Updated assignment files: {}", existingAssignmentFileDaoMap.keySet());
            existingAssignmentFileDaoMap.forEach((refId, dao) -> dao.update(newAssignmentFileRequestMapInitial.get(refId)));
        }

        log.debug("Added assignment files: {}", newAssignmentFileRequestMap.keySet());
        newAssignmentFileRequestMap.values().forEach(x -> assignmentDaoFactory.submissionAssignmentFileDao(this, null).create(x));
        refresh();
    }

    boolean canChangeAssignment() {
        return getStatus() == Assignment.Status.DRAFT || (getStatus() == Assignment.Status.INPROGRESS && getSubmissions().isEmpty());
    }

    private boolean canChangeMetadata() {
        return switch (getStatus()) {
            case CANCELED, FINISHED -> false;
            default -> true;
        };
    }

    @Override
    @PreAuthorize("hasPermission(#root.this.getNodeId(), T(org.edu_sharing.repository.client.tools.CCConstants).PERMISSION_ASSIGNMENT_COORDINATOR)")
    public void delete() {
        if (!exists()) {
            return;
        }

        // TODO can the user do this action
        if (canChangeAssignment()) {
            throw new IllegalStateException("Cannot delete assignment " + nodeId);
        }
        doDelete();
        refresh();
    }

    @Override
    public Assignment getAssignment() {
        if (!exists()) {
            return null;
        }

        String creator = getCreator();
        List<Submission> submissions = getSubmissions().stream().map(SubmissionDao::getSubmission).toList();
        return new Assignment(
                getNodeRef(),
                getTitle(),
                getSummary(),
                UserSimple.create(authorityService.getUser(creator), creator),
                getCreateDate(),
                getEndDate(),
                submissions.stream().allMatch(x -> x.submissionStatus().equals(Submission.Status.FINISHED)),
                getStatus(),
                getType(),
                getAllowAdditionalDocumentSubmissions(),
                getModifiedDate(),
                getPermissions(),
                submissions
        );
    }

    @Override
    public List<Assignment.Permission> getPermissions() {
        return permissions.get();
    }


    @Override
    public Boolean getAllowAdditionalDocumentSubmissions() {
        return propertyMapper.get().getBoolean(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, false);
    }

    @Override
    public Assignment.Type getType() {
        return Assignment.Type.SUBMISSION;
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

    public Collection<SubmissionDao> getSubmissions() {
        return submissions.get();
    }

    @Override
    public SubmissionDao getSubmission(String submissionId) {
        if ("-me-".equalsIgnoreCase(submissionId)) {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            return getSubmissionByCreator(currentUser)
                    .orElseThrow(() -> new MissingResourceException("No submission found for user " + currentUser));
        }

        SubmissionDao submissionDao = submissionsMap.get().get(submissionId);
        if (submissionDao == null) {
            throw new IllegalArgumentException("Submission with id " + submissionId + " does not exist.");
        }
        return submissionDao;
    }

    private Optional<SubmissionDao> getSubmissionByCreator(String creator) {
        return AuthenticationUtil.runAsSystem(() -> submissions.get()
                .stream()
                .filter(x -> x.getCreator().equals(creator))
                .findFirst());
    }

    @Override
    public SubmissionDao getOrCreateSubmission(String submissionId) {
        submissionsMap.invalidate();

        SubmissionDao submissionDao;
        if (StringUtils.isBlank(submissionId)) {
            submissionDao = assignmentDaoFactory.submissionDaoByNodeId(this, null);
            submissionDao.create();
            submissionsMap.get().put(submissionDao.getNodeId(), submissionDao);
        } else if ("-me-".equalsIgnoreCase(submissionId)) {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            Optional<SubmissionDao> submissionByCreator = getSubmissionByCreator(currentUser);
            if (submissionByCreator.isEmpty() || submissionByCreator.get() instanceof EmptySubmissionAssignmentDao) {
                submissionDao = assignmentDaoFactory.submissionDaoByNodeId(this, null);
                submissionDao.create();
                submissionsMap.get().put(submissionDao.getNodeId(), submissionDao);
            } else {
                submissionDao = submissionByCreator.get();
            }
        } else {
            submissionDao = submissionsMap.get().get(submissionId);
        }

        if (submissionDao == null) {
            throw new IllegalArgumentException("Submission with id " + submissionId + " does not exist.");
        }

        return submissionDao;
    }

    @Override
    public String getSubmissionRefId() {
        return submissionFolderRef.get();
    }

    @Override
    public AssignmentFileDao getAssignmentFile(String id) {
        AssignmentFileDao assignmentFileDao = assignmentFileRefs.get().get(id);
        if (assignmentFileDao == null) {
            throw new IllegalArgumentException("AssignmentFile with id " + id + " does not exist.");
        }
        return assignmentFileDao;
    }
}
