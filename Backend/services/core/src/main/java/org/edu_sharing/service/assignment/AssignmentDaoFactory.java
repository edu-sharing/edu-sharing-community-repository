package org.edu_sharing.service.assignment;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.assignment.v1.model.*;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.util.PropertyMapper;
import org.edu_sharing.util.CheckedSupplier;
import org.edu_sharing.util.LazyProvider;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class AssignmentDaoFactory {

    private final AssignmentConfig assignmentConfig;
    private final NodeService nodeService;
    private final org.alfresco.service.cmr.repository.NodeService alfNodeService;
    private final UserEnvironmentTool userEnvironmentTool;
    private final AuthorityService authorityService;
    private final PermissionService permissionService;
    private final SearchService searchService;

    private final LazyProvider<RepositoryDao> repositoryDao = new LazyProvider<>(RepositoryDao::getHomeRepository);
    private final ContentService contentService;

    /**
     * Retrieves an instance of {@link AssignmentDao} based on the provided node identifier.
     * The method creates and returns a prototype-scoped {@link AssignmentDao} implementation
     * initialized with the specified node ID.
     *
     * @param nodeId the unique identifier of the node for which the {@link AssignmentDao} is to be created
     * @return an instance of {@link AssignmentDao}, specific to the provided node ID
     */
    @Bean(autowireCandidate = false)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AssignmentDao assignment(String nodeId) {
        return new AssignmentDaoImpl(nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected AssignmentFileDao assignmentFile(AssignmentDao assignmentDao, String nodeId) {
        return new AssignmentFileDaoImpl(assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected SubmissionDao submissionDao(AssignmentDao assignmentDao, String nodeId) {
        return new SubmissionDaoImpl(assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected SubmissionFileDao submissionFileDao(AssignmentDao assignmentDao, SubmissionDao submissionDao, String nodeId) {
        return new SubmissionFileDaoImpl(assignmentDao, submissionDao, nodeId);
    }


    public SearchResult<AssignmentDao> searchAssignments(List<MdsQueryCriteria> searchCriteria, SearchToken searchToken) throws Throwable {
        Map<String, String[]> criteriaMap = MetadataSearchHelper.convertCriterias(searchCriteria);
        SearchResultNodeRef result = searchService.search(MetadataHelper.getLocalDefaultMetadataset(), "assignments", criteriaMap, searchToken);
        SearchResult<AssignmentDao> converted = new SearchResult<>();
        Pagination pagination = new Pagination();
        pagination.setFrom(result.getStartIDX());
        pagination.setTotal(result.getNodeCount());
        pagination.setCount(result.getData().size());
        converted.setFacets(result.getFacets());
        converted.setPagination(pagination);
        converted.setNodes(result.getData().stream().map(AssignmentDaoImpl::new).collect(Collectors.toList()));
        return converted;
    }


    protected void validateIsAssignmentCoordinator(String nodeId) {
        if (!isAssignmentCoordinator(nodeId)) {
            throw new AccessDeniedException("User is not allowed to perform this action");
        }
    }

    protected void validateIsAssignee(String nodeId) {
        if (!isAssignee(nodeId)) {
            throw new AccessDeniedException("User is not allowed to perform this action");
        }
    }

    protected boolean isAssignmentCoordinator(String nodeId) {
        if (AuthorityServiceHelper.isAdmin()) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR);
    }

    protected boolean isAssignee(String nodeId) {
        if (AuthorityServiceHelper.isAdmin()) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNEE);
    }


    @Getter
    protected abstract class BasicNodeDaoImpl implements BasicNodeDao {
        protected String nodeId;
        protected final LazyProvider<PropertyMapper> propertyMapper;

        public BasicNodeDaoImpl(String nodeId) {
            this(nodeId, Optional.empty());
        }

        public BasicNodeDaoImpl(String nodeId, Optional<org.edu_sharing.service.model.NodeRef> nodeRef) {
            this.nodeId = nodeId;
            propertyMapper = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                Map<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId());
                return new PropertyMapper(properties);
            }), nodeRef.map(x -> new PropertyMapper(x.getProperties())).orElse(null));
        }

        public boolean exists() {
            return StringUtils.isNotBlank(nodeId) && nodeService.exists(nodeId);
        }

        @Override
        public void delete() {
            try {
                log.debug("Deleting {} {}", this.getClass().getSimpleName(), nodeId);
                nodeService.removeNode(nodeId, null, true);
            } catch (Throwable t) {
                log.error("Error while deleting {}", this.getClass().getSimpleName(), t);
                throw new RuntimeException(t);
            }
        }


        @Override
        public String getCreator() {
            return propertyMapper.get().getString(CCConstants.CM_PROP_C_CREATOR);
        }

        @Override
        public Date getModifiedDate() {
            return propertyMapper.get().getDate(CCConstants.CM_PROP_C_MODIFIED);
        }

        @Override
        public Date getCreateDate() {
            return propertyMapper.get().getDate(CCConstants.CM_PROP_C_CREATED);
        }

        protected void validateExists() {
            if (!exists()) {
                throw new IllegalArgumentException("Assignment with id " + nodeId + " does not exist.");
            }
        }

        @NotNull
        @Override
        public NodeRef getNodeRef() {
            return new NodeRef(repositoryDao.get(), nodeId);
        }
    }

    protected final class AssignmentDaoImpl extends BasicNodeDaoImpl implements AssignmentDao {

        private final LazyProvider<Map<String, AssignmentFileDao>> assignmentFileRefs;
        private final LazyProvider<List<Assignment.Permission>> permissions;
        private final LazyProvider<Optional<String>> submissionFolderRef;
        private final LazyProvider<Map<String, SubmissionDao>> submissions;


        public AssignmentDaoImpl(String nodeId) {
            this(nodeId, Optional.empty());
        }

        public AssignmentDaoImpl(org.edu_sharing.service.model.NodeRef nodeRef) {
            this(nodeRef.getNodeId(), Optional.of(nodeRef));
        }

        private AssignmentDaoImpl(String nodeId, Optional<org.edu_sharing.service.model.NodeRef> nodeRef) {
            super(nodeId, nodeRef);
            assignmentFileRefs = new LazyProvider<>(() -> {
                validateExists();
                return nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_ASSOC_ASSIGNMENT_FILES)
                        .stream()
                        .map(ChildAssociationRef::getChildRef)
                        .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                        .map(x -> assignmentFile(this, x))
                        .collect(Collectors.toMap(AssignmentFileDao::getNodeId, x -> x));
            });

            permissions = new LazyProvider<>(() -> {
                validateExists();
                try {
                    return Arrays.stream(permissionService.getPermissions(getNodeId()).getAces())
                            .map(ace -> new Assignment.Permission(new Authority(ace), switch (ace.getPermission()) {
                                case CCConstants.PERMISSION_ASSIGNEE -> Assignment.Role.ASSIGNEE;
                                case CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR -> Assignment.Role.COORDINATOR;
                                default -> {
                                    log.error("Unknown permission {}", ace.getPermission());
                                    yield null;
                                }
                            }))
                            .toList();
                } catch (AccessDeniedException ignore) {
                    return Collections.emptyList();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            submissionFolderRef = new LazyProvider<>(() -> {
                validateExists();
                return nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_ASSOC_ASSIGNMENT_SUBMISSIONS)
                        .stream()
                        .findFirst()
                        .map(ChildAssociationRef::getChildRef)
                        .map(org.alfresco.service.cmr.repository.NodeRef::getId);
            });

            submissions = new LazyProvider<>(() -> {
                validateExists();
                return submissionFolderRef.get()
                        .map(subFolderId -> nodeService.getChildrenChildAssociationRefType(subFolderId, CCConstants.CCM_ASSOC_SUBMISSIONS_SUBMISSION)
                                .stream()
                                .map(ChildAssociationRef::getChildRef)
                                .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                                .collect(Collectors.toMap(x -> x, x -> (SubmissionDao) new SubmissionDaoImpl(this, x))))
                        .orElse(Collections.emptyMap());
            });
        }

        @Override
        @RunAsSystem
        @RetryingTransaction
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
                validateIsAssignmentCoordinator(nodeId);
                log.debug("Update assignment node {} with {}", nodeId, properties);
                nodeService.updateNodeNative(nodeId, properties);
            } else {
                try {
                    String assignmentFolder = userEnvironmentTool.getEdu_SharingAssignmentFolder();
                    String parentFolder = NodeServiceHelper.getContainerId(assignmentFolder, assignmentConfig.getNodePattern());
                    nodeId = nodeService.createNodeBasic(parentFolder, CCConstants.CCM_TYPE_ASSIGNMENT, properties);
                    log.debug("Created assignment node {}", nodeId);

                    // subfolder for submissions + permission only for type SUBMISSION
                    if (request.type() == Assignment.Type.SUBMISSION) {
                        Map<String, Object> submissionsProperties = Map.of(
                                CCConstants.CM_NAME, "submissions",// NodeRef
                                CCConstants.CM_PROP_TITLE, "Submissions"
                        );
                        String submissionId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CCM_TYPE_SUBMISSIONS, CCConstants.CCM_ASSOC_ASSIGNMENT_SUBMISSIONS, submissionsProperties);
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
                List<ACE> aceList = request.permissions()
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
                        .toList();
                log.debug("Setting permissions for assignment {}: {}", nodeId, aceList);
                permissionService.setPermissions(nodeId, aceList, false);
            } catch (Exception t) {
                throw new RuntimeException(t);
            }

            submissionFolderRef.get().ifPresent(submissionsNodeId -> {
                try {
                    List<ACE> aceList = request.permissions()
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
                            .toList();
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
            AssignmentDaoImpl assignmentDao = new AssignmentDaoImpl(nodeId);
            newAssignmentFileRequestMap.keySet().removeAll(assignmentFileDaoMap.keySet());
            log.debug("Added assignment files: {}", newAssignmentFileRequestMap.keySet());
            newAssignmentFileRequestMap.values().forEach(x -> assignmentFile(assignmentDao, null).create(x));
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
                    getPermissions()
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
                        .orElseThrow(() -> new IllegalArgumentException("No submission found for user " + currentUser));
            }

            SubmissionDao submissionDao = submissions.get().get(submissionId);
            if (submissionDao == null) {
                throw new IllegalArgumentException("Submission with id " + submissionId + " does not exist.");
            }
            return submissionDao;
        }

        private Optional<SubmissionDao> getSubmissionByCreator(String creator) {
            return submissions.get()
                    .values()
                    .stream()
                    .filter(x -> x.getCreator().equals(creator))
                    .findFirst();
        }

        @Override
        public SubmissionDao getOrCreateSubmission(String submissionId) {
            submissions.invalidate();

            if ("-me-".equalsIgnoreCase(submissionId)) {
                String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
                Optional<SubmissionDao> submissionByCreator = getSubmissionByCreator(currentUser);
                if (submissionByCreator.isEmpty()) {
                    SubmissionDao submissionDao = submissionDao(this, null);
                    submissionDao.create();
                    submissions.get().put(submissionDao.getNodeId(), submissionDao);
                }
            }

            return submissions.get().computeIfAbsent(submissionId, x -> {
                SubmissionDao submissionDao = submissionDao(this, x);
                submissionDao.create();
                return submissionDao;
            });
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
            return assignmentFileRefs.get().get(id);
        }

    }

    protected final class AssignmentFileDaoImpl extends BasicNodeDaoImpl implements AssignmentFileDao {
        private String nodeId;
        private final AssignmentDao assignmentDao;

        private final LazyProvider<PropertyMapper> propertyMapper;
        private final LazyProvider<Node> referNode;

        public AssignmentFileDaoImpl(AssignmentDao assignmentDao, String nodeId) {
            super(nodeId);
            this.assignmentDao = assignmentDao;

            propertyMapper = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                Map<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId());
                return new PropertyMapper(properties);
            }));
            referNode = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                return Optional.ofNullable(propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO))
                        .map(NodeDao::getAsNodeSimple)
                        .orElse(null);
            }));
        }

        public AssignmentFileDaoImpl(AssignmentDaoImpl assignmentDao) {
            this(assignmentDao, null);
        }

        @Override
        @RunAsSystem
        @RetryingTransaction
        public void create(AssignmentFileRequest request) {
            validateIsAssignmentCoordinator(assignmentDao.getNodeId());

            if (StringUtils.isNotBlank(nodeId)) {
                throw new IllegalStateException("AssignmentFile with id " + nodeId + " already exists.");
            }

            log.debug("Creating new assignment file");
            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, request.documentRole().name());
                put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.refId()));
            }};

            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, assignmentDao.getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE, CCConstants.CCM_ASSOC_ASSIGNMENT_FILES, properties);
            log.debug("Created new assignment file {} to {}", nodeId, assignmentDao.getNodeId());
            handleReferenceCopy(request, null, properties);
            log.debug("Updated properties for new assignment file {} with {}", nodeId, properties);
            nodeService.updateNodeNative(nodeId, properties);
        }

        @Override
        public void refresh() {
            propertyMapper.invalidate();
            referNode.invalidate();
        }

        @Override
        public AssignmentFile getAssignmentFile() {
            if (!exists()) {
                return null;
            }

            return new AssignmentFile(
                    new NodeRef(repositoryDao.get(), nodeId),
                    referNode.get(),
                    getDocumentRole(),
                    isDone());
        }

        @Override
        public String getReferNodeId() {
            return propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO).getId();
        }


        @Override
        public void update(@NonNull AssignmentFileRequest assignmentFileRequest) {
            validateExists();
            validateIsAssignmentCoordinator(nodeId);

            Map<String, Object> properties = new HashMap<>();
            if (!Objects.equals(assignmentFileRequest.refId(), getReferNodeId())) {
                handleReferenceCopy(assignmentFileRequest, getReferNodeId(), properties);
                properties.putIfAbsent(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, new NodeRef(StoreRef.PROTOCOL_WORKSPACE, assignmentFileRequest.refId()));
            }

            if (!Objects.equals(assignmentFileRequest.documentRole(), getDocumentRole())) {
                properties.put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, assignmentFileRequest.documentRole().name());
            }

            if (!Objects.equals(assignmentFileRequest.isDone(), isDone())) {
                properties.put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_IS_DONE, assignmentFileRequest.isDone());
            }

            if (!properties.isEmpty()) {
                nodeService.updateNodeNative(nodeId, properties);
                refresh();
            }
        }

        private void handleReferenceCopy(@NotNull AssignmentFileRequest assignmentFileRequest, String currentReferNodeId, Map<String, Object> properties) {
            if (assignmentDao.getType() != Assignment.Type.SUBMISSION) {
                log.debug("Skipping reference copy for non-submission assignment");
                return;
            }

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
            log.debug("Copied reference node {}", nodeRef.getId());
            properties.put(CCConstants.CCM_PROP_ASSIGNMENT_FILE_REFER_TO, nodeRef);
            nodeService.addAspect(nodeRef.getId(), CCConstants.CCM_ASPECT_ASSIGNMENT_FILE_DEEP_COPY);
            log.debug("Added deep copy aspect to reference node {}", nodeRef.getId());
        }

        @Override
        public Boolean isDone() {
            return propertyMapper.get().getBoolean(CCConstants.CCM_PROP_ASSIGNMENT_FILE_IS_DONE, false);
        }

        @Override
        public AssignmentFile.Role getDocumentRole() {
            return propertyMapper.get().getEnum(CCConstants.CCM_PROP_ASSIGNMENT_FILE_DOCUMENT_TYPE, AssignmentFile.Role.class);
        }
    }

    protected final class SubmissionDaoImpl extends BasicNodeDaoImpl implements SubmissionDao {

        private final AssignmentDao assignmentDao;

        public SubmissionDaoImpl(AssignmentDao assignmentDao) {
            this(assignmentDao, null);
        }

        public SubmissionDaoImpl(AssignmentDao assignmentDao, String nodeId) {
            super(nodeId);
            this.assignmentDao = assignmentDao;
        }

        @Override
        public void refresh() {
            propertyMapper.invalidate();
        }

        @Override
        public Submission getSubmission() {
            if (!exists()) {
                return null;
            }

            String creator = getCreator();
            if (isAssignmentCoordinator(nodeId)) {
                return new Submission(
                        getNodeRef(),
                        UserSimple.create(authorityService.getUser(creator), creator),
                        getValidationNotes(),
                        getFeedback(),
                        getStatus(),
                        getValidationStatus()
                );
            } else {
                return new Submission(
                        getNodeRef(),
                        UserSimple.create(authorityService.getUser(creator), creator),
                        null,
                        returned() ? getFeedback() : null,
                        getStatus(),
                        returned() ? getValidationStatus() : Submission.Status.PENDING
                );
            }
        }

        public boolean returned() {
            return getValidationStatus() == Submission.Status.FINISHED;
        }

        @Override
        public void update(EditSubmissionRequest request) {
            validateExists();
            refresh();

            validateIsAssignmentCoordinator(assignmentDao.getNodeId());
            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, request.validationStatus().name());
                put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_NOTES, request.validationNotes());
                put(CCConstants.CCM_PROP_SUBMISSION_FEEDBACK, request.feedback());
            }};

            nodeService.updateNodeNative(nodeId, properties);
            refresh();
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

            if (currentStatus == Submission.Status.FINISHED && !isAssignmentCoordinator(nodeId)) {
                throw new IllegalStateException("Submission with id " + nodeId + " is already finished.");
            }

            nodeService.updateNodeNative(nodeId, Map.of(CCConstants.CCM_PROP_SUBMISSION_STATUS, status.name()));
            refresh();
        }

        @Override
        public List<SubmissionFileDao> getSubmissionFiles() {
            return List.of();
        }

        @Override
        public SubmissionFileDao getSubmissionFile(String submissionFileId) {
            return null;
        }

        @Override
        @RunAsSystem
        @RetryingTransaction
        public SubmissionFileDao createOrUpdateSubmissionFile(String submissionFileId, SubmissionFileRequest submissionFileRequest, InputStream fileInputStream, FormDataContentDisposition fileMetaData) {
            return null;
        }

        @Override
        @RunAsSystem
        @RetryingTransaction
        public void create() {
            if (StringUtils.isNotBlank(nodeId)) {
                throw new IllegalStateException("Submission with id " + getNodeId() + " already exists.");
            }

            validateIsAssignee(assignmentDao.getNodeId());

            log.debug("Creating new submission");
            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_SUBMISSION_STATUS, Submission.Status.NOT_STARTET.name());
                put(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.NOT_STARTET.name());
            }};

            String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();

            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, assignmentDao.getSubmissionRefId(), CCConstants.CCM_TYPE_SUBMISSION, CCConstants.CCM_ASSOC_SUBMISSIONS_SUBMISSION, properties);
            log.debug("Created new submission for {}({}) to {}", fullyAuthenticatedUser, nodeId, assignmentDao.getSubmissionRefId());

            permissionService.setPermission(nodeId, fullyAuthenticatedUser, CCConstants.PERMISSION_ASSIGNEE);
            log.debug("Added permission {} for {} to submission {}", CCConstants.PERMISSION_ASSIGNEE, fullyAuthenticatedUser, nodeId);
        }

        @Override
        public Submission.Status getStatus() {
            return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_STATUS, Submission.Status.class);
        }

        @Override
        public Submission.Status getValidationStatus() {
            return propertyMapper.get().getEnum(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, Submission.Status.class);
        }

        @Override
        public String getFeedback() {
            return propertyMapper.get().getString(CCConstants.CCM_PROP_SUBMISSION_FEEDBACK);
        }

        @Override
        public String getValidationNotes() {
            return propertyMapper.get().getString(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_NOTES);
        }
    }

    protected final class SubmissionFileDaoImpl extends BasicNodeDaoImpl implements SubmissionFileDao {

        private final AssignmentDao assignmentDao;
        private final SubmissionDao submissionDao;

        private final LazyProvider<org.alfresco.service.cmr.repository.NodeRef> contentNodeId;
        private final LazyProvider<Node> contentNode;


        public SubmissionFileDaoImpl(AssignmentDao assignmentDao, SubmissionDao submissionDao, String nodeId) {
            super(nodeId);
            this.assignmentDao = assignmentDao;
            this.submissionDao = submissionDao;

            contentNodeId = new LazyProvider<>(() -> {
                validateExists();
                return nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_ASSOC_SUBMISSION_FILE_DATA)
                        .stream()
                        .map(ChildAssociationRef::getChildRef)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No content node found for submission file " + nodeId));
            });

            contentNode = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                return Optional.ofNullable(contentNodeId.get())
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
        @RunAsSystem
        @RetryingTransaction
        public void create(SubmissionFileRequest request, InputStream fileInputStream) {
            validateCanAssigneeChangeSubmission();

            if (StringUtils.isNotBlank(nodeId)) {
                throw new IllegalStateException("Submission file with id " + nodeId + " already exists.");
            }

            log.debug("Creating new submission file");
            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.assignmentFile()));
            }};
            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, submissionDao.getNodeId(), CCConstants.CCM_TYPE_SUBMISSION_FILE, CCConstants.CCM_ASSOC_SUBMISSION_FILES, properties);
            log.debug("Created new submission file for {} to {}", nodeId, submissionDao.getNodeId());
            handleSubmissionFile(request, fileInputStream);
        }

        @RunAsSystem
        @RetryingTransaction
        public void update(SubmissionFileRequest request, InputStream fileInputStream) {
            refresh();
            validateExists();

            validateCanAssigneeChangeSubmission();

            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.assignmentFile()));
            }};

            nodeService.updateNodeNative(nodeId, properties);
            handleSubmissionFile(request, fileInputStream);
        }

        private void handleSubmissionFile(SubmissionFileRequest request, InputStream fileInputStream) {
            if (StringUtils.isNotBlank(request.originalFile()) && fileInputStream != null) {
                throw new IllegalArgumentException("Cannot create submission file with original file and file input stream");
            }

            if (StringUtils.isNotBlank(getContentNodeId()) && nodeService.exists(getContentNodeId())) {
                log.debug("Deleting old content node {}", getContentNodeId());
                nodeService.removeNode(getContentNodeId(), nodeId, false);
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

            if (permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), request.originalFile(), CCConstants.PERMISSION_DOWNLOAD_CONTENT)) {
                throw new InsufficientPermissionException("You do not have permission to copy the original file. Required permission: " + CCConstants.PERMISSION_DOWNLOAD_CONTENT);
            }

            log.debug("Copying reference node {}", request.originalFile());
            org.alfresco.service.cmr.repository.NodeRef contentNodeRef = nodeService.copyNode(request.originalFile(), nodeId, CCConstants.CCM_ASSOC_SUBMISSION_FILE_DATA, true);
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
        }

        private void handleFileUpload(SubmissionFileRequest request, InputStream fileInputStream) {
            String contentNodeId;
            try {
                Map<String, String[]> contentProperties = new HashMap<>(request.properties()) {{
                    put(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE, new String[]{request.assignmentFile()});
                }};
                contentNodeId = nodeService.createNode(nodeId, CCConstants.CCM_TYPE_SUBMISSION_FILE, contentProperties, CCConstants.CCM_ASSOC_SUBMISSION_FILES, true);
                log.debug("Created new submission file content node {} for {}", contentNodeId, nodeId);

                ContentWriter writer = contentService.getWriter(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), ContentModel.PROP_CONTENT, true);
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
        }

        private void validateCanAssigneeChangeSubmission() {
            validateIsAssignee(nodeId);

            if (assignmentDao.getEndDate() != null && assignmentDao.getEndDate().before(new Date())) {
                throw new InsufficientPermissionException("Assignment with id " + assignmentDao.getNodeId() + " has already ended.");
            }

            if (submissionDao.getStatus() == Submission.Status.FINISHED) {
                throw new InsufficientPermissionException("Submission with id " + submissionDao.getNodeId() + " has already been finished.");
            }
        }

        private void validateCanCoordinatorChangeSubmission() {
            validateIsAssignmentCoordinator(nodeId);

            if (submissionDao.getStatus() == Submission.Status.FINISHED) {
                return;
            }

            if (assignmentDao.getEndDate() != null && assignmentDao.getEndDate().after(new Date())) {
                throw new InsufficientPermissionException("Assignment with id " + assignmentDao.getNodeId() + " has not yet been completed.");
            }
        }

        @Override
        public void setValidationStatus(Submission.Status validationStatus) {
            validateExists();
            validateCanCoordinatorChangeSubmission();
            refresh();

            nodeService.updateNodeNative(nodeId, Map.of(CCConstants.CCM_PROP_SUBMISSION_VALIDATION_STATUS, validationStatus.name()));
            refresh();
        }

        @Override
        public void delete() {
            validateExists();
            validateCanCoordinatorChangeSubmission();
        }

        @Override
        public SubmissionFile getSubmissionFile() {
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
            org.alfresco.service.cmr.repository.NodeRef nodeRef = propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_SUBMISSION_FILE_REFER_TO_ASSIGNMENT_FIlE);
            if (nodeRef == null) {
                return Optional.empty();
            }
            return Optional.of(assignmentDao.getAssignmentFile(nodeRef.getId()));
        }

        @Override
        public String getContentNodeId() {
            return contentNodeId.get().getId();
        }

    }

}

