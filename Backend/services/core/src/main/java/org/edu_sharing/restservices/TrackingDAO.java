package org.edu_sharing.restservices;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import com.drew.lang.annotations.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.permission.annotation.HasRole;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.tracking.ActivityStatisticService;
import org.edu_sharing.service.tracking.GroupingType;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequestScope
@RequiredArgsConstructor
public class TrackingDAO {

    private final ActivityStatisticService activityStatisticService;
    private final SearchServiceElastic searchService;

    private final RepositoryDao homeRepository = RepositoryDao.getHomeRepository();
    private final Filter filter = Filter.createShowAllFilter();

    @Value( "${repository.statistics.searchResultsLimit:50000}")
    private int maxSearchResults;

    public List<TrackingNode> getNodeStatistics(GroupingType grouping, Date fromDate, Date toDate, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws DAOException {

        try {
            List<StatisticEntryNode> tracks = activityStatisticService.getNodeStatisics(grouping, fromDate, toDate, mediacenter, additionalFields, groupFields, filters);
            List<TrackingNode> result = new ArrayList<>();
            if (tracks != null) {
                for (StatisticEntryNode track : tracks) {
                    Node node = null;
                    if (track.getNode() != null) {
                        try {
                            node = NodeDao.getAsNodeSimple(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, track.getNode()));
                        } catch (DAOMissingException e) {
                            // node is propably deleted, only set the ref
                            node = new Node();
                            node.setRef(new NodeRef(RepositoryDao.getHomeRepository(), track.getNode()));
                        }
                    }
                    TrackingNode tracking = new TrackingNode(node, convertAuthority(track.getAuthorityInfo()), track.getDate(), track.getCounts(), track.getFields(), track.getGroups());
                    result.add(tracking);
                }
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public List<Tracking> getUserStatistics(GroupingType grouping, Date fromDate, Date toDate, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws DAOException {

        try {
            List<StatisticEntry> tracks = activityStatisticService.getUserStatistics(grouping, fromDate, toDate, mediacenter, additionalFields, groupFields, filters);
            List<Tracking> result = new ArrayList<>();
            if (tracks != null) {
                for (StatisticEntry track : tracks) {
                    Tracking tracking = new Tracking(track.getDate(), convertAuthority(track.getAuthorityInfo()), track.getCounts(), track.getFields(), track.getGroups());
                    result.add(tracking);
                }
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private Tracking.TrackingAuthority convertAuthority(StatisticEntry.AuthorityInfo authorityInfo) {
        if (authorityInfo == null)
            return null;
        Tracking.TrackingAuthority trackingAuthority = new Tracking.TrackingAuthority();
        trackingAuthority.setHash(authorityInfo.getAuthority());
        if (authorityInfo.getOrganizations() != null) {
            trackingAuthority.setOrganization(Arrays.stream(authorityInfo.getOrganizations()).map((organiation) ->
            {
                try {
                    return OrganizationDao.getInstant(RepositoryDao.getHomeRepository(), organiation).asOrganization();
                } catch (DAOException e) {
                    Organization o = new Organization();
                    o.setAuthorityName(organiation);
                    return o;
                }
            }).collect(Collectors.toList()));
        }
        if (authorityInfo.getMediacenters() != null) {
            trackingAuthority.setMediacenter(Arrays.stream(authorityInfo.getMediacenters()).map((mediacenter) ->
            {
                try {
                    return GroupDao.getGroup(RepositoryDao.getHomeRepository(), mediacenter).asGroup();
                } catch (DAOException e) {
                    Group g = new Group();
                    g.setAuthorityName(mediacenter);
                    return g;
                }
            }).collect(Collectors.toList()));
        }
        return trackingAuthority;
    }

    public List<String> getNodesAltered(Date dateFrom) {
        return activityStatisticService.getAlteredNodes(dateFrom);
    }

    public List<NodeData> getNodeData(String nodeId, Date dateFrom) {
        return activityStatisticService.getNodeData(nodeId, dateFrom);
    }

    @NotNull
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_USER_STATISTICS_NODES)
    public List<TrackingNode> getNodeStatisticsByOwningUser(@NotNull @NonNull String userId, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {
        BoolQuery.Builder filter = QueryBuilders.bool()
                .must(m -> m.term(t -> t.field("owner").value(userId)))
                .must(m -> m.term(t -> t.field("type").value("ccm:io")));

        if (publishedOnly) {
            filter.must(m->m.bool(searchService::getPublishedPermissionsQuery));
        }

        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }


    @NotNull
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_SELECTIVE_STATISTICS_NODES)
    public List<TrackingNode> getNodeStatisticsByRange(@NotNull @NonNull List<String> nodeIds, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {
        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(50000);
        BoolQuery.Builder filter = QueryBuilders.bool()
                .must(m -> m.bool(searchService::getCoordinatorPermissionsQuery))
                .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                .must(m -> m.bool(b -> b
                        .should(nodeIds.stream().map(id -> QueryBuilders.term(t -> t.field("path").value(id))).toList())
                        .should(nodeIds.stream().map(id -> QueryBuilders.term(t -> t.field("_id").value(id))).toList())
                ));

        if (publishedOnly) {
            filter.must(m->m.bool(searchService::getPublishedPermissionsQuery));
        }

        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }

    private org.alfresco.service.cmr.repository.NodeRef getOriginalNodeRef(org.edu_sharing.service.model.NodeRef nodeRef) {

        org.alfresco.service.cmr.repository.NodeRef originalNodeRef = nodeRef.asAlfrescoNodeRef();
        if (nodeRef.getAspects().contains(CCConstants.CCM_ASPECT_PUBLISHED)) {
            originalNodeRef = org.alfresco.service.cmr.repository.NodeRef.getNodeRefs((String) nodeRef.getProperties().get(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL))
                    .stream()
                    .findFirst()
                    .orElse(originalNodeRef);
        } else if (nodeRef.getAspects().contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
            originalNodeRef = new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String) nodeRef.getProperties().getOrDefault(CCConstants.CCM_PROP_IO_ORIGINAL, nodeRef.getNodeId()));
        }
        return originalNodeRef;
    }

    @NotNull
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_ORGANIZATION_STATISTICS_NODES)
    public List<TrackingNode> getNodeStatisticsByOrganization(@HasRole @NotNull @NonNull String orgId, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {
        BoolQuery.Builder filter = QueryBuilders.bool()
                .must(m -> m.bool(searchService::getReadPermissionsQuery))
                .must(m -> m.term(t -> t.field("properties.ccm:owning_organisation.keyword").value(orgId)))
                .must(m -> m.term(t -> t.field("type").value("ccm:io")));

        if (publishedOnly) {
            filter.must(m->m.bool(searchService::getPublishedPermissionsQuery));
        }

        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }

    @NotNull
    private List<TrackingNode> searchBasedStatisticEvaluation(@NotNull @NonNull QueryVariant query, @NotNull @NonNull Date startDate, @NotNull @NonNull Date endDate, int maxResults) throws Throwable {
        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(maxSearchResults); // physical limit because of db execution time
        searchToken.setElasticQuery(query);

        SearchResultNodeRef search = searchService.search(searchToken);
        List<org.alfresco.service.cmr.repository.NodeRef> nodeRefs = search.getData()
                .stream()
                .map(this::getOriginalNodeRef)
                .distinct()
                .toList();

        Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> trackingMap = activityStatisticService.getListNodeData(nodeRefs, startDate, endDate, null, null);

        return search.getData()
                .stream()
                .map(nodeRef -> new AbstractMap.SimpleEntry<>(nodeRef, trackingMap.get(getOriginalNodeRef(nodeRef))))
                .sorted(Comparator.comparing(this::getTotalCounts))
                .limit(maxResults)
                .map(this::map)
                .toList();
    }

    private int getTotalCounts(@NotNull Map.Entry<org.edu_sharing.service.model.NodeRef, StatisticEntry> entry) {
        return entry.getValue().getCounts().values().stream().reduce(Integer::sum).orElse(0);
    }

    @NotNull
    private TrackingNode map(@NotNull AbstractMap.SimpleEntry<org.edu_sharing.service.model.NodeRef, StatisticEntry> entry) {
        StatisticEntry statisticEntry = entry.getValue();
        Node node = new NodeDao(homeRepository, entry.getKey(), filter).asNode();
        return new TrackingNode(node, convertAuthority(statisticEntry.getAuthorityInfo()), statisticEntry.getDate(), statisticEntry.getCounts(), statisticEntry.getFields(), statisticEntry.getGroups());
    }
}
