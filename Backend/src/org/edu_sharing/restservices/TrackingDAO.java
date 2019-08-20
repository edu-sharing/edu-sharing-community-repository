package org.edu_sharing.restservices;

import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackingDAO {
    public static List<TrackingNode> getNodeStatistics(Date fromDate, Date toDate) throws DAOException {
        List<StatisticEntryNode> tracks = TrackingServiceFactory.getTrackingService().getNodeStatisics(fromDate,toDate);
        List<TrackingNode> result = new ArrayList<>();
        if(tracks!=null) {
            for (StatisticEntryNode track : tracks) {
                result.add(new TrackingNode(NodeDao.getNode(RepositoryDao.getHomeRepository(), track.getNode()).asNode(), track.getCounts()));
            }
        }
        return result;
    }
}
