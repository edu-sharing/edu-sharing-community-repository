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
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;
import org.edu_sharing.restservices.assignment.v1.model.CreateAssignmentRequest;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
final class AssignmentDaoImpl extends BasicNodeDaoImpl implements AssignmentDao {

    private final LazyProvider<Map<String, AssignmentFileDao>> assignmentFileRefs;
    private final LazyProvider<List<Assignment.Permission>> permissions;
    private final LazyProvider<Optional<String>> submissionFolderRef;
    private final LazyProvider<Map<String, SubmissionDao>> submissions;

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

    public AssignmentDaoImpl(String nodeId) {
        this(nodeId, Optional.empty());
    }

    public AssignmentDaoImpl(org.edu_sharing.service.model.NodeRef nodeRef) {
        this(nodeRef.getNodeId(), Optional.of(nodeRef));
    }

    private AssignmentDaoImpl(String nodeId, Optional<NodeRef> nodeRef) {
        super(nodeId, nodeRef);

        // TODO tracker submissions as nested
        // TODO user relations from nodeRef?
        assignmentFileRefs = new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE)
                    .stream()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                    .map(x -> assignmentDaoFactory.assignmentFileDao(this, x))
                    .collect(Collectors.toMap(AssignmentFileDao::getNodeId, x -> x)));
        });

        permissions = new LazyProvider<>(() -> {
            validateExists();
            if (!AssignmentUtil.isAssignmentCoordinator(permissionService, getNodeId())) {
                return Collections.emptyList();
            }

            return AuthenticationUtil.runAsSystem(CheckedRunAsWork.wrap(() ->
                    Arrays.stream(permissionService.getPermissions(getNodeId()).getAces())
                            .map(ace -> new Assignment.Permission(new Authority(ace), switch (ace.getPermission()) {
                                // filter consumer role
                                case CCConstants.PERMISSION_CONSUMER -> null;
                                case CCConstants.PERMISSION_ASSIGNEE -> Assignment.Role.ASSIGNEE;
                                case CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR -> Assignment.Role.COORDINATOR;
                                default -> {
                                    log.error("Unknown permission for assignment {} {}", nodeId, ace.getPermission());
                                    yield null;
                                }
                            }))
                            .filter(p -> Objects.nonNull(p.role()))
                            .toList()
            ));
        });

        submissionFolderRef = new LazyProvider<>(() -> {
            validateExists();
            return AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_SUBMISSIONS)
                    .stream()
                    .findFirst()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId));
        });

        submissions = new LazyProvider<>(() -> {
            validateExists();

            List<ChildAssociationRef> childAssociationRefs = AuthenticationUtil.runAsSystem(() -> submissionFolderRef.get()
                    .map(subFolderId -> nodeService.getChildrenChildAssociationRefType(subFolderId, CCConstants.CCM_TYPE_SUBMISSION))
                    .orElse(Collections.emptyList()));

            return childAssociationRefs.stream()
                    .map(ChildAssociationRef::getChildRef)
                    .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                    .filter(id -> AssignmentUtil.hasAccessTo(permissionService, id))
                    .collect(Collectors.toMap(x -> x, x -> assignmentDaoFactory.submissionDao(this, x)));
        });
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
            put(CCConstants.CCM_PROP_ASSIGNMENT_TYPE, request.type().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_STATUS, request.status().name());
            put(CCConstants.CCM_PROP_ASSIGNMENT_ALLOW_ADDITIONAL_DOCUMENT_SUBMISSIONS, request.allowAdditionalDocumentSubmissions());
            put(CCConstants.CCM_PROP_ASSIGNMENT_END_DATE, request.endTime());
        }};

        if (StringUtils.isNotBlank(nodeId)) {
            validateExists();

            switch (getStatus()) {
                case FINISHED:
                case CANCELED:
                    throw new IllegalStateException("Assignment with id " + nodeId + " is not in status OPEN, cannot update");
            }

            log.debug("Update assignment node {} with {}", nodeId, properties);
            nodeService.updateNodeNative(nodeId, properties);
        } else {
            try {
                String assignmentFolder = userEnvironmentTool.getEdu_SharingAssignmentFolder();
                String parentFolder = NodeServiceHelper.getContainerId(assignmentFolder, assignmentConfig.getNodePattern());
                nodeId = nodeService.createNodeBasic(parentFolder, CCConstants.CCM_TYPE_ASSIGNMENT, properties);
                nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
                log.debug("Created assignment node {}", nodeId);

                // subfolder for submissions + permission only for type SUBMISSION
                if (request.type() == Assignment.Type.SUBMISSION) {
                    Map<String, Object> submissionsProperties = Map.of(
                            CCConstants.CM_NAME, "submissions",// NodeRef
                            CCConstants.CM_PROP_TITLE, "Submissions"
                    );
                    String submissionId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CCM_TYPE_SUBMISSIONS, CCConstants.CCM_ASSOC_ASSIGNMENT_SUBMISSIONS, submissionsProperties);
                    nodeService.setOwner(submissionId, ApplicationInfoList.getHomeRepository().getUsername());
                    log.debug("Created submissions node {}", submissionId);
                }

            } catch (Throwable t) {
                log.error("Error while creating assignment", t);
                throw new RuntimeException(t);
            }
        }

        // check permissions can only contains assignee if type is SUBMISSION
        if (request.type() != Assignment.Type.SUBMISSION) {
            if (request.permissions().stream().anyMatch(x -> x.role() == Assignment.Role.ASSIGNEE)) {
                throw new IllegalArgumentException("Permissions can only contain assignee for type SUBMISSION");
            }
        }

        try {
            List<ACE> aceList = new ArrayList<>(request.permissions()
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
        } catch (Exception t) {
            throw new RuntimeException(t);
        }

        submissionFolderRef.get().ifPresent(submissionsNodeId -> {
            try {
                List<ACE> aceList = new ArrayList<>(request.permissions()
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
                log.debug("Setting permissions for submissions folder {}: {}", submissionsNodeId, aceList);
                permissionService.setPermissions(submissionsNodeId, aceList, false);
            } catch (Exception t) {
                log.error("Error while setting permissions for submissions", t);
                throw new RuntimeException(t);
            }
        });


        updateAssignmentFiles(request);
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
        assignmentFileDaoMap.keySet().retainAll(newAssignmentFileRequestMap.keySet());

        // update assignment files that are in the request
        log.debug("Updated assignment files: {}", assignmentFileDaoMap.keySet());
        assignmentFileDaoMap.forEach((refId, dao) -> dao.update(newAssignmentFileRequestMap.get(refId)));

        // add new assignment files
        newAssignmentFileRequestMap.keySet().removeAll(assignmentFileDaoMap.keySet());
        log.debug("Added assignment files: {}", newAssignmentFileRequestMap.keySet());
        newAssignmentFileRequestMap.values().forEach(x -> assignmentDaoFactory.assignmentFileDao(this, null).create(x));
    }

    @Override
    public void refresh() {
        log.debug("Refreshing assignment {}", nodeId);
        propertyMapper.invalidate();
        assignmentFileRefs.invalidate();
        submissionFolderRef.invalidate();
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
                getStatus(),
                getType(),
                getAllowAdditionalDocumentSubmissions(),
                getModifiedDate(),
                getPermissions(),
                // TODO filter by permission
                submissions
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
        return submissions.get().values();
    }

    @Override
    public SubmissionDao getSubmission(String submissionId) {
        if ("-me-".equalsIgnoreCase(submissionId)) {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            return getSubmissionByCreator(currentUser)
                    .orElseThrow(() -> new MissingResourceException("No submission found for user " + currentUser));
        }

        SubmissionDao submissionDao = submissions.get().get(submissionId);
        if (submissionDao == null) {
            throw new IllegalArgumentException("Submission with id " + submissionId + " does not exist.");
        }
        return submissionDao;
    }

    private Optional<SubmissionDao> getSubmissionByCreator(String creator) {
        return AuthenticationUtil.runAsSystem(() -> submissions.get()
                .values()
                .stream()
                .filter(x -> x.getCreator().equals(creator))
                .findFirst());
    }

    @Override
    public SubmissionDao getOrCreateSubmission(String submissionId) {
        submissions.invalidate();

        SubmissionDao submissionDao;
        if (StringUtils.isBlank(submissionId)) {
            submissionDao = assignmentDaoFactory.submissionDao(this, null);
            submissionDao.create();
            submissions.get().put(submissionDao.getNodeId(), submissionDao);
        } else if ("-me-".equalsIgnoreCase(submissionId)) {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            Optional<SubmissionDao> submissionByCreator = getSubmissionByCreator(currentUser);
            if (submissionByCreator.isEmpty()) {
                submissionDao = assignmentDaoFactory.submissionDao(this, null);
                submissionDao.create();
                submissions.get().put(submissionDao.getNodeId(), submissionDao);
            } else {
                submissionDao = submissionByCreator.get();
            }
        } else {
            submissionDao = submissions.get().get(submissionId);
        }

        if (submissionDao == null) {
            throw new IllegalArgumentException("Submission with id " + submissionId + " does not exist.");
        }

        return submissionDao;
    }


    public List<Assignment.Permission> getPermissions() {
        return permissions.get();
    }

    @Override
    public String getSubmissionRefId() {
        return submissionFolderRef.get().orElse(null);
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
