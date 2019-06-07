package org.edu_sharing.restservices;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackingDAO {
    public static List<TrackingNode> getNodeStatistics(TrackingService.GroupingType grouping,Date fromDate, Date toDate) throws DAOException {
        List<StatisticEntryNode> tracks = TrackingServiceFactory.getTrackingService().getNodeStatisics(grouping,fromDate,toDate);
        List<TrackingNode> result = new ArrayList<>();
        if(tracks!=null) {
            for (StatisticEntryNode track : tracks) {
                Node node=null;
                if(track.getNode()!=null)
                    node=NodeDao.getNode(RepositoryDao.getHomeRepository(), track.getNode()).asNode();

                TrackingNode tracking = new TrackingNode(node, track.getDate(), track.getCounts());
                result.add(tracking);
            }
        }
        return result;
    }
}
