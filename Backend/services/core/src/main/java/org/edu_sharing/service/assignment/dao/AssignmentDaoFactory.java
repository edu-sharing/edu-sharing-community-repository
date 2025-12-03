package org.edu_sharing.service.assignment.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.assignment.*;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
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
    public AssignmentDao assignmentDaoByNodeId(String nodeId) {
        return new AssignmentDaoImpl(nodeId);
    }

    @Bean(autowireCandidate = false)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AssignmentDao assignmentDaoByNodeRef(org.edu_sharing.service.model.NodeRef nodeId) {
        return new AssignmentDaoImpl(nodeId);
    }



    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    AssignmentFileDao assignmentFileDao(AssignmentDaoImpl assignmentDao, String nodeId) {
        return new AssignmentFileDaoImpl(assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SubmissionDao submissionDao(AssignmentDaoImpl assignmentDao, String nodeId) {
        return new SubmissionDaoImpl(this, assignmentDao, nodeId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SubmissionFileDao submissionFileDao(AssignmentDaoImpl assignmentDao, SubmissionDaoImpl submissionDao, String nodeId) {
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
        converted.setNodes(result.getData().stream().map(this::assignmentDaoByNodeRef).collect(Collectors.toList()));
        return converted;
    }
}

