package org.edu_sharing.restservices;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TrackingDAO {
    public static List<TrackingNode> getNodeStatistics(TrackingService.GroupingType grouping, Date fromDate, Date toDate,List<String> additionalFields,List<String> groupFields, Map<String,String> filters) throws DAOException {

        try {
            List<StatisticEntryNode> tracks = TrackingServiceFactory.getTrackingService().getNodeStatisics(grouping,fromDate,toDate,additionalFields,groupFields,filters);
            List<TrackingNode> result = new ArrayList<>();
            if(tracks!=null) {
                for (StatisticEntryNode track : tracks) {
                    Node node=null;
                    if(track.getNode()!=null) {
                        try {
                            node = NodeDao.getNode(RepositoryDao.getHomeRepository(), track.getNode()).asNode();
                        }catch(DAOMissingException e){
                            // node is propably deleted, only set the ref
                            node = new Node();
                            node.setRef(new NodeRef(RepositoryDao.getHomeRepository(),track.getNode()));
                        }
                    }
                    TrackingNode tracking = new TrackingNode(node, track.getAuthority(), track.getDate(), track.getCounts(),track.getData());
                    result.add(tracking);
                }
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }
    public static List<Tracking> getUserStatistics(TrackingService.GroupingType grouping, Date fromDate, Date toDate, List<String> additionalFields, List<String> groupFields, Map<String,String> filters) throws DAOException {

        try {
            List<StatisticEntry> tracks = TrackingServiceFactory.getTrackingService().getUserStatistics(grouping,fromDate,toDate,additionalFields,groupFields,filters);
            List<Tracking> result = new ArrayList<>();
            if(tracks!=null) {
                for (StatisticEntry track : tracks) {
                    Tracking tracking = new Tracking(track.getDate(),track.getAuthority(), track.getCounts(),track.getData());
                    result.add(tracking);
                }
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }
}
