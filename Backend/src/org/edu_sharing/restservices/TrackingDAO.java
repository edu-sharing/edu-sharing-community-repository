package org.edu_sharing.restservices;

import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.*;
import java.util.stream.Collectors;

public class TrackingDAO {
    public static List<TrackingNode> getNodeStatistics(TrackingService.GroupingType grouping, Date fromDate, Date toDate, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws DAOException {

        try {
            List<StatisticEntryNode> tracks = TrackingServiceFactory.getTrackingService().getNodeStatisics(grouping,fromDate,toDate,mediacenter,new TrackingService.StatisticsFetchConfig(additionalFields,groupFields,filters));
            List<TrackingNode> result = new ArrayList<>();
            if(tracks!=null) {
                addNodeData(tracks, result);
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private static void addNodeData(List<StatisticEntryNode> tracks, List<TrackingNode> result) throws DAOException {
        NodeDao.FetchConfig fetchConfig = new NodeDao.FetchConfig();
        fetchConfig.setFetchPermissions(false);
        fetchConfig.setFetchPersons(false);
        fetchConfig.setFetchReference(false);
        fetchConfig.setFetchRemote(false);
        for (StatisticEntryNode track : tracks) {
            Node node=null;
            if(track.getNode()!=null) {
                try {

                    NodeDao dao = NodeDao.getNode(RepositoryDao.getHomeRepository(), track.getNode(), Filter.createShowAllFilter(), fetchConfig);
                    dao.fetchCounts = false;
                    node = dao.asNode();
                }catch(DAOMissingException e){
                    // node is propably deleted, only set the ref
                    node = new Node();
                    node.setRef(new NodeRef(RepositoryDao.getHomeRepository(),track.getNode()));
                }
            }
            TrackingNode tracking = new TrackingNode(node, convertAuthority(track.getAuthorityInfo()), track.getDate(), track.getCounts(),track.getFields(),track.getGroups());
            result.add(tracking);
        }
    }

    public static List<TrackingNode> getNodeStatisticsAll(Date fromDate, Date toDate, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws DAOException {
        try {
            List<StatisticEntryNode> tracks = TrackingServiceFactory.getTrackingService().getNodeStatisics(TrackingService.GroupingType.Node,fromDate,toDate,mediacenter,new TrackingService.StatisticsFetchConfig(additionalFields,groupFields,filters,true));
            List<TrackingNode> result = new ArrayList<>();
            if(tracks!=null) {
                addNodeData(tracks, result);
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }

    }
    public static List<Tracking> getUserStatistics(TrackingService.GroupingType grouping, Date fromDate, Date toDate, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String,String> filters) throws DAOException {

        try {
            List<StatisticEntry> tracks = TrackingServiceFactory.getTrackingService().getUserStatistics(grouping,fromDate,toDate,mediacenter,new TrackingService.StatisticsFetchConfig(additionalFields,groupFields,filters,false));
            List<Tracking> result = new ArrayList<>();
            if(tracks!=null) {
                for (StatisticEntry track : tracks) {
                    Tracking tracking = new Tracking(track.getDate(),convertAuthority(track.getAuthorityInfo()), track.getCounts(),track.getFields(),track.getGroups());
                    result.add(tracking);
                }
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private static Tracking.TrackingAuthority convertAuthority(StatisticEntry.AuthorityInfo authorityInfo) {
        if(authorityInfo==null)
            return null;
        Tracking.TrackingAuthority trackingAuthority = new Tracking.TrackingAuthority();
        trackingAuthority.setHash(authorityInfo.getAuthority());
        if(authorityInfo.getOrganizations()!=null) {
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
        if(authorityInfo.getMediacenters()!=null) {
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

    public static List<String> getNodesAltered(Date dateFrom) {
        return TrackingServiceFactory.getTrackingService().getAlteredNodes(dateFrom);
    }

    public static List<NodeData> getNodeData(String nodeId, Date dateFrom) {
        return TrackingServiceFactory.getTrackingService().getNodeData(nodeId, dateFrom);
    }
}
