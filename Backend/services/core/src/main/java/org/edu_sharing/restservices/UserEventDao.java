package org.edu_sharing.restservices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SearchUserEvent;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class UserEventDao {

    private final RepositoryDao repoDao;
    private final NodeDao nodeDao;
    private final SearchUserEvent event;

    public UserEventDao(RepositoryDao repoDao, SearchUserEvent event) {
        this.repoDao = repoDao;
        this.nodeDao = new NodeDao(repoDao, event.getNodeRef(), Filter.createShowAllFilter());
        this.event = event;
    }

    public static SearchResult<UserEventDao> getRecentUserEvents(RepositoryDao repoDao, List<ActivityOnNodeEventType> filter, List<MdsQueryCriteria> searchCriteria, SearchToken searchToken) throws DAOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
            SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
            try {
                Map<String, String[]> criteriaMap = MetadataSearchHelper.convertCriterias(searchCriteria);
                org.edu_sharing.repository.server.SearchResult<SearchUserEvent> result = searchService.getRecentUserEvents(filter, criteriaMap, searchToken);

                SearchResult<UserEventDao> converted = new SearchResult<>();
                Pagination pagination = new Pagination();
                pagination.setFrom(result.getStartIDX());
                pagination.setTotal(result.getNodeCount());
                pagination.setCount(result.getData().size());
                converted.setFacets(result.getFacets());
                converted.setPagination(pagination);
                converted.setNodes(result.getData().stream().map((ref) -> {
                    try {
                        return new UserEventDao(
                                repoDao, ref
                        );
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

    public UserEvent asUserEvent() {
        return new UserEvent(
                this.nodeDao.asNode(),
                PersonDao.getPerson(this.repoDao, this.event.getInitiator()).asPersonSimple(false),
                this.event.getTimestamp(),
                this.event.getEventType()
        );
    }

    @Data
    @AllArgsConstructor
    public static class UserEvent {
        private Node node;
        private UserSimple initiator;
        private Date timestamp;
        private ActivityOnNodeEventType eventType;
    }
}
