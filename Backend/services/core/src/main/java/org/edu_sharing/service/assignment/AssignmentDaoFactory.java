package org.edu_sharing.service.assignment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
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
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFile;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;
import org.edu_sharing.restservices.assignment.v1.model.CreateAssignmentRequest;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.util.PropertyMapper;
import org.edu_sharing.util.CheckedSupplier;
import org.edu_sharing.util.LazyProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AssignmentDaoFactory {

    private final AssignmentConfig assignmentConfig;
    private final NodeService nodeService;
    private final UserEnvironmentTool userEnvironmentTool;
    private final AuthorityService authorityService;
    private final PermissionService permissionService;
    private final SearchService searchService;

    private final LazyProvider<RepositoryDao> repositoryDao = new LazyProvider<>(RepositoryDao::getHomeRepository);

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
    public AssignmentDao getAssignment(String nodeId) {
        return new AssignmentDaoImpl(nodeId);
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
        converted.setNodes(result.getData().stream().map(x -> new AssignmentDaoImpl(x)).collect(Collectors.toList()));
        return converted;
    }

    private final class AssignmentDaoImpl implements AssignmentDao {

        private String nodeId;

        private final LazyProvider<PropertyMapper> propertyMapper;
        private final LazyProvider<List<AssignmentFileDao>> assignmentFileRefs;
        private final LazyProvider<List<Assignment.Permission>> permissions;
        private final LazyProvider<Optional<String>> submissionFolderRef;

        public AssignmentDaoImpl(String nodeId) {
            this(nodeId, Optional.empty());
        }

        public AssignmentDaoImpl(org.edu_sharing.service.model.NodeRef nodeRef) {
            this(nodeRef.getNodeId(), Optional.of(nodeRef));
        }

        private AssignmentDaoImpl(String nodeId, Optional<org.edu_sharing.service.model.NodeRef> nodeRef) {
            this.nodeId = nodeId;
            propertyMapper = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                Map<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId());
                return new PropertyMapper(properties);
            }), nodeRef.map(x -> new PropertyMapper(x.getProperties())).orElse(null));

            assignmentFileRefs = new LazyProvider<>(() -> {
                validateExists();
                return nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE)
                        .stream()
                        .map(ChildAssociationRef::getChildRef)
                        .map(org.alfresco.service.cmr.repository.NodeRef::getId)
                        .map(x -> new AssignmentFileDaoImpl(this, x))
                        .map(AssignmentFileDao.class::cast)
                        .toList();
            });

            permissions = new LazyProvider<>(() -> {
                validateExists();
                try {
                    return Arrays.stream(permissionService.getPermissions(getNodeId()).getAces())
                            .map(ace -> new Assignment.Permission(new Authority(ace), switch (ace.getPermission()) {
                                case CCConstants.PERMISSION_READ -> Assignment.Role.ASSIGNEE;
                                case CCConstants.PERMISSION_COORDINATOR -> Assignment.Role.COORDINATOR;
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
                return nodeService.getChildrenChildAssociationRefType(getNodeId(), CCConstants.CCM_TYPE_SUBMISSIONS)
                        .stream()
                        .findFirst()
                        .map(ChildAssociationRef::getChildRef)
                        .map(org.alfresco.service.cmr.repository.NodeRef::getId);
            });
        }


        private void validateExists() {
            if (!exists()) {
                throw new IllegalArgumentException("Assignment with id " + nodeId + " does not exist.");
            }
        }

        @Override
        @RetryingTransaction
        public void createOrUpdate(CreateAssignmentRequest request) {
            Map<String, Object> properties = Map.of(
                    CCConstants.CM_NAME, UUID.randomUUID().toString(),
                    CCConstants.CM_PROP_TITLE, request.title(),
                    CCConstants.CM_PROP_DESCRIPTION, request.summary(),
                    CCConstants.CCM_PROP_ASSIGNMENT_TYPE, request.type().name(),
                    CCConstants.CCM_PROP_ASSIGNMENT_STATUS, request.status().name(),
                    CCConstants.CCM_PROP_ASSIGNMENT_ALLOWADDITIONALDOCUMENTSUBMISSION, request.allowAdditionalDocumentSubmission(),
                    CCConstants.CCM_PROP_ASSIGNMENT_ENDDATE, request.endTime()
            );

            if (StringUtils.isNotBlank(nodeId)) {
                validateExists();
                log.debug("Update assignment node {} with {}", nodeId, properties);
                nodeService.updateNodeNative(nodeId, properties);
            } else {
                try {
                    String assignmentFolder = userEnvironmentTool.getEdu_SharingAssignmentFolder();
                    String parentFolder = NodeServiceHelper.getContainerId(assignmentFolder, assignmentConfig.getNodePattern());
                    nodeId = nodeService.createNodeBasic(parentFolder, CCConstants.CCM_TYPE_ASSIGNMENT, properties);
                    log.debug("Created assignment node {}", nodeId);
                    // TODO subfolder for submissions + permission only for type SUBMISSION
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
                                            .map(y -> new ACE(CCConstants.PERMISSION_READ, y));

                                } else {
                                    yield Stream.of(new ACE(CCConstants.PERMISSION_READ, x.authorityName()));
                                }
                            }
                            case COORDINATOR ->
                                    Stream.of(new ACE(CCConstants.PERMISSION_COORDINATOR, x.authorityName()));
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
                                        case COORDINATOR -> new ACE(CCConstants.PERMISSION_READ, x.authorityName());
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
            newAssignmentFileRequestMap.values().forEach(x -> new AssignmentFileDaoImpl(assignmentDao).create(x));
        }

        @Override
        public void refresh() {
            log.debug("Refreshing assignment {}", nodeId);
            propertyMapper.invalidate();
            assignmentFileRefs.invalidate();
            submissionFolderRef.invalidate();
        }

        @Override
        public boolean exists() {
            return StringUtils.isNotBlank(nodeId) && nodeService.exists(nodeId);
        }

        @Override
        public void delete() {
            try {
                log.debug("Deleting assignment {}", nodeId);
                nodeService.removeNode(nodeId, null, true);
            } catch (Throwable t) {
                log.error("Error while deleting assignment", t);
                throw new RuntimeException(t);
            }
        }

        @Override
        public Assignment getAssignment() {
            if (!exists()) {
                return null;
            }

            String creator = getCreator();
            return new Assignment(
                    new NodeRef(repositoryDao.get(), nodeId),
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
        public String getNodeId() {
            return nodeId;
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
        public Boolean getAllowAdditionalDocumentSubmissions() {
            return propertyMapper.get().getBoolean(CCConstants.CCM_PROP_ASSIGNMENT_ALLOWADDITIONALDOCUMENTSUBMISSION, false);
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
            return propertyMapper.get().getDate(CCConstants.CCM_PROP_ASSIGNMENT_ENDDATE);
        }

        @Override
        public Date getCreateDate() {
            return propertyMapper.get().getDate(CCConstants.CM_PROP_C_CREATED);
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
        public List<AssignmentFileDao> getAssignmentFiles() {
            return assignmentFileRefs.get();
        }


        public List<Assignment.Permission> getPermissions() {
            return permissions.get();
        }

    }


    private final class AssignmentFileDaoImpl implements AssignmentFileDao {
        private String nodeId;
        private final AssignmentDao assignmentDao;

        private final LazyProvider<PropertyMapper> propertyMapper;
        private final LazyProvider<Node> referNode;

        public AssignmentFileDaoImpl(AssignmentDao assignmentDao, String nodeId) {
            this.nodeId = nodeId;
            this.assignmentDao = assignmentDao;

            propertyMapper = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                Map<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId());
                return new PropertyMapper(properties);
            }));
            referNode = new LazyProvider<>(CheckedSupplier.wrap(() -> {
                validateExists();
                return Optional.ofNullable(propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENTFILE_REFERTO))
                        .map(NodeDao::getAsNodeSimple)
                        .orElse(null);
            }));
        }

        public AssignmentFileDaoImpl(AssignmentDao assignmentDao) {
            this(assignmentDao, null);
        }


        public void create(AssignmentFileRequest request) {
            if (StringUtils.isBlank(nodeId)) {
                throw new IllegalStateException("AssignmentFile with id " + nodeId + " already exists.");
            }

            log.debug("Creating new assignment file");
            Map<String, Object> properties = new HashMap<>() {{
                put(CCConstants.CM_NAME, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_ASSIGNMENTFILE_DOCUMENTTYPE, request.documentRole().name());
                put(CCConstants.CCM_PROP_ASSIGNMENTFILE_REFERTO, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, request.refId()));
            }};

            nodeId = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, assignmentDao.getNodeId(), CCConstants.CCM_TYPE_ASSIGNMENT_FILE, CCConstants.CCM_ASSOC_ASSIGNMENT_FILES, properties);
            log.debug("Created new assignment file {} to  {}", nodeId, assignmentDao.getNodeId());
            handleReferenceCopy(request, null, properties);
            log.debug("Updated properties for new assignment file {} with {}", nodeId, properties);
            nodeService.updateNodeNative(nodeId, properties);
        }

        private void validateExists() {
            if (!exists()) {
                throw new IllegalArgumentException("Assignment with id " + nodeId + " does not exist.");
            }
        }

        @Override
        public void refresh() {
            propertyMapper.invalidate();
            referNode.invalidate();
        }

        @Override
        public boolean exists() {
            return StringUtils.isNotBlank(nodeId) && nodeService.exists(nodeId);
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
        public void delete() {
            nodeService.removeNode(nodeId, null, true);
        }

        @Override
        public String getReferNodeId() {
            return propertyMapper.get().getNodeRef(CCConstants.CCM_PROP_ASSIGNMENTFILE_REFERTO).getId();
        }


        @Override
        public void update(@NonNull AssignmentFileRequest assignmentFileRequest) {
            Map<String, Object> properties = new HashMap<>();
            if (!Objects.equals(assignmentFileRequest.refId(), getReferNodeId())) {
                handleReferenceCopy(assignmentFileRequest, getReferNodeId(), properties);
                properties.putIfAbsent(CCConstants.CCM_PROP_ASSIGNMENTFILE_REFERTO, new NodeRef(StoreRef.PROTOCOL_WORKSPACE, assignmentFileRequest.refId()));
            }

            if (!Objects.equals(assignmentFileRequest.documentRole(), getDocumentRole())) {
                properties.put(CCConstants.CCM_PROP_ASSIGNMENTFILE_DOCUMENTTYPE, assignmentFileRequest.documentRole().name());
            }

            if (!Objects.equals(assignmentFileRequest.isDone(), isDone())) {
                properties.put(CCConstants.CCM_PROP_ASSIGNMENTFILE_ISDONE, assignmentFileRequest.isDone());
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

            try {
                log.debug("Copying reference node {}", assignmentFileRequest.refId());
                org.alfresco.service.cmr.repository.NodeRef nodeRef = nodeService.copyNode(assignmentFileRequest.refId(), nodeId, CCConstants.CCM_ASSOC_ASSIGNMENT_FILE_COPY, true);
                log.debug("Copied reference node {}", nodeRef.getId());
                properties.put(CCConstants.CCM_PROP_ASSIGNMENTFILE_REFERTO, nodeRef);
            } catch (Throwable t) {
                log.error("Error while copying node", t);
                throw new RuntimeException(t);
            }
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public Boolean isDone() {
            return propertyMapper.get().getBoolean(CCConstants.CCM_PROP_ASSIGNMENTFILE_ISDONE, false);
        }

        @Override
        public AssignmentFile.Role getDocumentRole() {
            return propertyMapper.get().getEnum(CCConstants.CCM_PROP_ASSIGNMENTFILE_DOCUMENTTYPE, AssignmentFile.Role.class);
        }
    }

}

