package org.edu_sharing.restservices;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import com.drew.lang.annotations.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.annotations.Queued;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.permission.annotation.HasRole;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.tracking.ActivityStatisticService;
import org.edu_sharing.service.tracking.GroupingType;
import org.edu_sharing.service.tracking.StatisticsFileService;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrackingDAO {

    private final ActivityStatisticService activityStatisticService;
    private final SearchServiceElastic searchService;
    private final StatisticsFileService statisticsFileService;
    private final NotificationService notificationService;

    private final Filter filter = Filter.createShowAllFilter();

    @Value("${repository.statistics.searchResultsLimit:50000}")
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
    public Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> getNodeStatisticsByOwningUser(@HasRole @NotNull @NonNull String userId, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {

        // type = ccm:io && ((aspect = ccm:published && cm:creator = userId) ...)
        BoolQuery.Builder filter = QueryBuilders.bool()
                // filter only io's
                .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                .minimumShouldMatch("1")
                // in case originals of published copies are deleted (produces duplications in the result list!)
                .should(s -> s.bool(b -> b.must(m -> m.bool(b2 -> b2
                        .must(m2 -> m2.term(t -> t.field("aspects").value("ccm:published")))
                        .must(m2 -> m2.term(t -> t.field("properties.cm:creator").value(userId)))))));

        if (publishedOnly) {
            // ... || (owner = userId && (permissions.read == GROUP_EVERYONE || ccm:published_mode=copy))
            filter.should(s -> s.bool(b -> b
                    // filter by owner
                    .must(m -> m.term(t -> t.field("owner").value(userId)))
                    .minimumShouldMatch("1")
                    // nodes published directly
                    .should(s2 -> s2.bool(searchService::getPublishedPermissionsQuery))
                    // in case of published copies
                    .should(s2 -> s2.term(t -> t.field("properties.ccm:published_mode").value("copy")))));

        } else {
            // filter by owner
            filter.should(m -> m.term(t -> t.field("owner").value(userId)));
        }

        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }


    @NotNull
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_SELECTIVE_STATISTICS_NODES)
    public Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> getNodeStatisticsByRange(@NotNull @NonNull List<String> nodeIds, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {
        String me = AuthenticationUtil.getFullyAuthenticatedUser();

        // type = ccm:io && (nodeId in path or id = nodeId) &&((aspect = ccm:published && cm:creator = userId) ...)
        BoolQuery.Builder filter = QueryBuilders.bool()
                // filter only io's
                .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                // retrieve nodes inside folders or by id
                .must(m -> m.bool(b -> b
                        .should(nodeIds.stream().map(id -> QueryBuilders.term(t -> t.field("path").value(id))).toList())
                        .should(nodeIds.stream().map(id -> QueryBuilders.term(t -> t.field("_id").value(id))).toList())
                ))
                .minimumShouldMatch("1")
                // in case originals of published copies are deleted (produces duplications in the result list!)
                .should(s -> s.bool(b -> b.must(m -> m.bool(b2 -> b2
                        .must(m2 -> m2.term(t -> t.field("aspects").value("ccm:published")))
                        .must(m2 -> m2.term(t -> t.field("properties.cm:creator").value(me)))))));


        if (publishedOnly) {
            // ... || (permissions = coordinator && (permissions.read == GROUP_EVERYONE || published_mode = copy))
            filter.should(s -> s.bool(b -> b.must(m -> m.bool(searchService::getCoordinatorPermissionsQuery))
                    .minimumShouldMatch("1")
                    // nodes published directly
                    .should(s2 -> s2.bool(searchService::getPublishedPermissionsQuery))
                    // in case of published copies
                    .should(s2 -> s2.term(t -> t.field("properties.ccm:published_mode").value("copy")))));
        } else {
            // ... || permissions = coordinator
            filter.should(m -> m.bool(searchService::getCoordinatorPermissionsQuery));
        }
        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }


    @NotNull
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_ORGANIZATION_STATISTICS_NODES)
    public Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> getNodeStatisticsByOrganization(@HasRole @NotNull @NonNull String orgId, @NotNull @NonNull Date dateFrom, @NotNull @NonNull Date dateTo, int maxResults, boolean publishedOnly) throws Throwable {
        BoolQuery.Builder filter = QueryBuilders.bool()
                .must(m -> m.term(t -> t.field("type").value("ccm:io")))
                .must(m -> m.bool(searchService::getReadPermissionsQuery))
                .must(m -> m.term(t -> t.field("properties.ccm:owning_organisation.keyword").value(orgId)));

        if (publishedOnly) {
            filter.must(m -> m.bool(searchService::getPublishedPermissionsQuery));
        }

        return searchBasedStatisticEvaluation(filter.build(), dateFrom, dateTo, maxResults);
    }

    @NotNull
    private Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> searchBasedStatisticEvaluation(@NotNull @NonNull QueryVariant query, @NotNull @NonNull Date startDate, @NotNull @NonNull Date endDate, int maxResults) throws Throwable {
        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(maxSearchResults); // physical limit because of db execution time
        searchToken.setElasticQuery(query);

        SearchResultNodeRef search = searchService.search(searchToken);
        Map<org.alfresco.service.cmr.repository.NodeRef, List<org.edu_sharing.service.model.NodeRef>> nodeRefGroup = search.getData()
                .stream()
                .collect(Collectors.groupingBy(this::getOriginalNodeRef));

        List<org.alfresco.service.cmr.repository.NodeRef> nodeRefs = nodeRefGroup.keySet().stream().toList();
        Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> trackingMap = activityStatisticService.getListNodeData(nodeRefs, startDate, endDate, null, null);

        return trackingMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(this::getTotalCounts))
                .limit(maxResults)
                .map(x -> {
                    List<org.edu_sharing.service.model.NodeRef> dataList = nodeRefGroup.get(x.getKey());
                    if (dataList.size() == 1) {
                        return new AbstractMap.SimpleEntry<>(dataList.get(0), x.getValue());
                    } else {
                        // find the original node
                        Optional<org.edu_sharing.service.model.NodeRef> originalNode = dataList.stream().filter(y -> y.getNodeId().equals(x.getKey().getId())).findFirst();
                        if (originalNode.isPresent()) {
                            return new AbstractMap.SimpleEntry<>(originalNode.get(), x.getValue());
                        }

                        // find the latest published node or take the first in list
                        org.edu_sharing.service.model.NodeRef firstRefOrLatestPublishedCopy = dataList.stream().reduce((lhs, rhs) -> {
                            boolean lhsIsPublished = isPublished(lhs);
                            boolean rhsIsPublished = isPublished(rhs);
                            if (lhsIsPublished && rhsIsPublished) {
                                Date lhsDate = getPublishedDate(lhs);
                                Date rhsDate = getPublishedDate(rhs);
                                if (rhsDate == null) {
                                    return lhs;
                                }

                                if (lhsDate == null) {
                                    return rhs;
                                }

                                return lhsDate.after(rhsDate) ? lhs : rhs;
                            }

                            if (rhsIsPublished) {
                                return rhs;
                            }

                            return lhs;
                        }).orElseThrow();

                        return new AbstractMap.SimpleEntry<>(firstRefOrLatestPublishedCopy, x.getValue());
                    }

                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey,
                        AbstractMap.SimpleEntry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    private org.alfresco.service.cmr.repository.NodeRef getOriginalNodeRef(org.edu_sharing.service.model.NodeRef nodeRef) {

        org.alfresco.service.cmr.repository.NodeRef originalNodeRef = nodeRef.asAlfrescoNodeRef();
        if (isPublished(nodeRef)) {
            originalNodeRef = org.alfresco.service.cmr.repository.NodeRef.getNodeRefs((String) nodeRef.getProperties().get(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL))
                    .stream()
                    .findFirst()
                    .orElse(originalNodeRef);
        } else if (isCollectionIoReference(nodeRef)) {
            originalNodeRef = new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String) nodeRef.getProperties().getOrDefault(CCConstants.CCM_PROP_IO_ORIGINAL, nodeRef.getNodeId()));
        }
        return originalNodeRef;
    }

    private boolean isCollectionIoReference(org.edu_sharing.service.model.NodeRef nodeRef) {
        return nodeRef.getAspects().contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
    }

    private boolean isPublished(org.edu_sharing.service.model.NodeRef nodeRef) {
        return nodeRef.getAspects().contains(CCConstants.CCM_ASPECT_PUBLISHED);
    }

    private Date getPublishedDate(org.edu_sharing.service.model.NodeRef nodeRef) {
        return Optional.ofNullable(nodeRef.getProperties().get(CCConstants.CCM_PROP_PUBLISHED_DATE))
                .map(String.class::cast)
                .map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::parse)
                .map(Instant::from)
                .map(Date::from)
                .orElse(null);
    }

    private int getTotalCounts(@NotNull Map.Entry<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> entry) {
        return entry.getValue().getCounts().values().stream().reduce(Integer::sum).orElse(0);
    }

    @NotNull
    public List<TrackingNode> map(@NotNull Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> data) {
        RepositoryDao homeRepository = RepositoryDao.getHomeRepository();
        return data.entrySet()
                .stream()
                .map(x -> map(x, homeRepository))
                .collect(Collectors.toList());
    }


    @NotNull
    public TrackingNode map(@NotNull Map.Entry<org.edu_sharing.service.model.NodeRef, StatisticEntry> entry, @NotNull RepositoryDao homeRepository) {
        StatisticEntry statisticEntry = entry.getValue();
        Node node = new NodeDao(homeRepository, entry.getKey(), filter).asNode();
        return new TrackingNode(node, convertAuthority(statisticEntry.getAuthorityInfo()), statisticEntry.getDate(), statisticEntry.getCounts(), statisticEntry.getFields(), statisticEntry.getGroups());
    }

    @Queued(unique = true)
    public void scheduleNodeStatisticsByRange(List<String> nodeIds, Date startDate, Date endDate, boolean publishedOnly, List<List<String>> properties) {
        try {
            Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> statisticEntryMap = getNodeStatisticsByRange(nodeIds, startDate, endDate, Integer.MAX_VALUE, publishedOnly);
            String userInboxNodeId = RepositoryDao.getHomeRepository().getUserInbox(true);
            String filename = getFilename(startDate, endDate,  I18nAngular.getTranslationAngular("common", "STATISTICS.SELECTIVE_MATERIALS"));
            statisticsFileService.writeCSV(userInboxNodeId, filename, statisticEntryMap, properties);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static String getFilename(Date startDate, Date endDate, String postfix) {
        // TODO i18n
        return String.join("_",
                startDate.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                , I18nAngular.getTranslationAngular("common", "STATISTICS.TO")
                , endDate.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                , postfix).replace(":", "_");
    }

    @Queued(unique = true)
    public void scheduleNodeStatisticsByOrganization(@HasRole String orgId, Date startDate, Date endDate, boolean publishedOnly, List<List<String>> properties) {
        try {
            Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> statisticEntryMap = getNodeStatisticsByOrganization(orgId, startDate, endDate, Integer.MAX_VALUE, publishedOnly);
            String userInboxNodeId = RepositoryDao.getHomeRepository().getUserInbox(true);
            // TODO i18n
            String filename = getFilename(startDate, endDate, orgId + "_" + I18nAngular.getTranslationAngular("common", "STATISTICS.MATERIALS"));
            statisticsFileService.writeCSV(userInboxNodeId, filename, statisticEntryMap, properties);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Queued(unique = true)
    public void scheduleNodeStatisticsByOwningUser(@HasRole String userId, Date startDate, Date endDate, boolean publishedOnly, List<List<String>> properties) {
        try {
            Map<org.edu_sharing.service.model.NodeRef, StatisticEntry> statisticEntryMap = getNodeStatisticsByOwningUser(userId, startDate, endDate, Integer.MAX_VALUE, publishedOnly);
            String userInboxNodeId = RepositoryDao.getHomeRepository().getUserInbox(true);
            // TODO i18n
            String filename = getFilename(startDate, endDate, userId + "_" + I18nAngular.getTranslationAngular("common", "STATISTICS.MATERIALS"));
            statisticsFileService.writeCSV(userInboxNodeId, filename, statisticEntryMap, properties);
            //notificationService.notifyInbox()
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
