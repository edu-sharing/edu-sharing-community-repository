package org.edu_sharing.restservices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.generated.repository.backend.services.rest.client.model.ShareInfo;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.UserShareDirection;
import org.edu_sharing.service.search.model.SearchInviteEvent;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class UserShareDao {

    private final RepositoryDao repoDao;
    private final NodeDao nodeDao;
    private final SearchInviteEvent event;

    public UserShareDao(RepositoryDao repoDao, SearchInviteEvent event) {
        this.repoDao = repoDao;
        this.nodeDao = new NodeDao(repoDao, event.getNodeRef(), Filter.createShowAllFilter());
        this.event = event;
    }

    public static SearchResult<UserShareDao> getUserShares(RepositoryDao repoDao, UserShareDirection direction, List<MdsQueryCriteria> searchCriteria, SearchToken searchToken) throws DAOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
            SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
            try {
                Map<String, String[]> criteriaMap = MetadataSearchHelper.convertCriterias(searchCriteria);
                org.edu_sharing.repository.server.SearchResult<SearchInviteEvent> result = searchService.getUserShares(direction, criteriaMap, searchToken);

                SearchResult<UserShareDao> converted = new SearchResult<>();
                Pagination pagination = new Pagination();
                pagination.setFrom(result.getStartIDX());
                pagination.setTotal(result.getNodeCount());
                pagination.setCount(result.getData().size());
                converted.setFacets(result.getFacets());
                converted.setPagination(pagination);
                converted.setNodes(result.getData().stream().map((ref) -> {
                    try {
                        return new UserShareDao(
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

    public InviteEvent asUserEvent() {
        return new InviteEvent(
                this.nodeDao.asNode(),
                PersonDao.getPerson(this.repoDao, this.event.getSharedBy()).asPersonSimple(false),
                PersonDao.getPerson(this.repoDao, this.event.getSharedWith()).asPersonSimple(false),
                this.event.getTimestamp(),
                this.event.getShareType(),
                this.event.getShareStatus()
        );
    }

    @Data
    @AllArgsConstructor
    public static class InviteEvent {
        private Node node;
        private UserSimple sharedBy;
        private UserSimple sharedWith;
        private Date timestamp;
        private ShareInfo.ShareTypeEnum shareType;
        private ShareInfo.ShareStatusEnum shareStatus;
    }
}
