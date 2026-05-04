package org.edu_sharing.restservices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchSuggestionNode;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class SuggestionDao {

    private final RepositoryDao repoDao;
    private final NodeDao nodeDao;
    private final SearchSuggestionNode event;

    public SuggestionDao(RepositoryDao repoDao, SearchSuggestionNode event) {
        this.repoDao = repoDao;
        this.nodeDao = new NodeDao(repoDao, event.getNodeRef(), Filter.createShowAllFilter());
        this.event = event;
    }

    public static SearchResult<SuggestionDao> getNodesBySuggestion(
            RepositoryDao repoDao,
            List<SuggestionStatus> statusFilter,
            List<SuggestionType> typeFilter,
            List<MdsQueryCriteria> searchCriteria,
            SearchToken searchToken) throws DAOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
            SearchService searchService = SearchServiceFactory.getInstance().getService(repoDao.getApplicationInfo().getAppId());
            try {
                Map<String, String[]> criteriaMap = MetadataSearchHelper.convertCriterias(searchCriteria);
                org.edu_sharing.repository.server.SearchResult<SearchSuggestionNode> result =
                        searchService.getNodesBySuggestion(statusFilter, typeFilter, criteriaMap, searchToken);

                SearchResult<SuggestionDao> converted = new SearchResult<>();
                Pagination pagination = new Pagination();
                pagination.setFrom(result.getStartIDX());
                pagination.setTotal(result.getNodeCount());
                pagination.setCount(result.getData().size());
                converted.setFacets(result.getFacets());
                converted.setPagination(pagination);
                converted.setNodes(result.getData().stream().map((ref) -> {
                    try {
                        return new SuggestionDao(repoDao, ref);
                    } catch (DAOException e) {
                        log.warn(e.getMessage(), e);
                    }
                    return null;
                }).collect(Collectors.toList()));
                return converted;

            } catch (Exception e) {
                throw DAOException.mapping(e);
            }
        });
    }

    public NodeSuggestionEntry asNodeSuggestionEvent() {
        return new NodeSuggestionEntry(
                this.nodeDao.asNode(),
                this.event.getSuggestions().stream().map(s -> new SuggestionNode(
                        s.getId(),
                        s.getType(),
                        s.getStatus(),
                        s.getPropertyId(),
                        s.getValue(),
                        s.getVersion(),
                        s.getDescription(),
                        s.getCreatedBy() != null ? PersonDao.getPerson(this.repoDao, s.getCreatedBy()).asPersonSimple(false) : null,
                        s.getCreated()
                )).collect(Collectors.toList())
        );
    }

    @Data
    @AllArgsConstructor
    public static class NodeSuggestionEntry {
        private Node node;
        private List<SuggestionNode> suggestionNodes;
    }

    @Data
    @AllArgsConstructor
    public static class SuggestionNode {
        private String id;
        private SuggestionType type;
        private SuggestionStatus status;
        private String propertyId;
        private String value;
        private String version;
        private String description;
        private UserSimple createdBy;
        private Date created;
    }
}
