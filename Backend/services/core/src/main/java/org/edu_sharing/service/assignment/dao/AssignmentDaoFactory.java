package org.edu_sharing.service.assignment.dao;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.assignment.*;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class AssignmentDaoFactory {

    private final NodeService nodeService;
    private final SearchService searchService;

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
    public AssignmentDao assignmentDaoByNodeId(@NotNull @NonNull String nodeId) {
        String type = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_PROP_ASSIGNMENT_TYPE);
        return switch (Enum.valueOf(Assignment.Type.class, type)) {
            case SUBMISSION -> new NodeSubmissionAssignmentDao(nodeId);
            case DEFAULT -> new NodeAssignmentDao(nodeId);
        };
    }

    @Bean(autowireCandidate = false)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AssignmentDao assignmentDaoByNodeRef(@NotNull @NonNull org.edu_sharing.service.model.NodeRef nodeRef) {
        String type = (String) nodeRef.getProperties().get(CCConstants.CCM_PROP_ASSIGNMENT_TYPE);
        return switch (Enum.valueOf(Assignment.Type.class, type)) {
            case SUBMISSION -> new NodeSubmissionAssignmentDao(nodeRef);
            case DEFAULT -> new NodeAssignmentDao(nodeRef);
        };
    }

    @Bean(autowireCandidate = false)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AssignmentDao assignemntDaoByType(Assignment.Type type) {
        return switch (type) {
            case SUBMISSION -> new NodeSubmissionAssignmentDao();
            case DEFAULT -> new NodeAssignmentDao();
        };
    }



    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    AssignmentFileDao assignmentFileDao(NodeAssignmentDao assignmentDao, String nodeId) {
        return new NodeAssignmentFileDao(assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    AssignmentFileDao submissionAssignmentFileDao(NodeSubmissionAssignmentDao assignmentDao, String nodeId) {
        return new NodeSubmissionAssignmentFileDao(assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SubmissionDao submissionDaoByNodeId(NodeSubmissionAssignmentDao assignmentDao, String nodeId) {
        return new NodeSubmissionDao(this, assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SubmissionDao submissionDaoByNodeRef(NodeSubmissionAssignmentDao assignmentDao, org.edu_sharing.service.model.NodeRef nodeRef) {
        return new NodeSubmissionDao(this, assignmentDao, nodeRef);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SubmissionDao emptySubmissionDao(String creator) {
        return new EmptySubmissionAssignmentDao(creator);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SubmissionFileDao submissionFileDao(NodeSubmissionAssignmentDao assignmentDao, NodeSubmissionDao submissionDao, String nodeId) {
        return new NodeSubmissionFileDao(assignmentDao, submissionDao, nodeId);
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
        converted.setNodes(result.getData().stream().map(this::assignmentDaoByNodeRef).collect(Collectors.toList()));
        return converted;
    }
}

