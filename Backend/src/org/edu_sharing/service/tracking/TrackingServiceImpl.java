package org.edu_sharing.service.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.springframework.context.ApplicationContext;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TrackingServiceImpl extends TrackingServiceDefault{
    public static String TRACKING_NODE_TABLE_ID = "edu_tracking_node";
    public static String TRACKING_USER_TABLE_ID = "edu_tracking_user";
    public static String TRACKING_INSERT_NODE = "insert into " + TRACKING_NODE_TABLE_ID +" (node_id,node_uuid,node_version,authority,time,type,data) VALUES (?,?,?,?,?,?,?)";
    public static String TRACKING_INSERT_USER = "insert into " + TRACKING_USER_TABLE_ID +" VALUES (?,?,?,?)";
    public static String TRACKING_STATISTICS_NODE = "SELECT node_uuid as node,type,COUNT(*) from edu_tracking_node as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ?" +
            " GROUP BY node,type" +
            " ORDER BY count DESC";
    private final NodeService nodeService;

    public TrackingServiceImpl() {
        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

        ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService=serviceRegistry.getNodeService();
    }

    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        super.trackActivityOnUser(authorityName,type);
        if(authorityName==null ||authorityName.equals(ApplicationInfoList.getHomeRepository().getGuest_username())){
            return false;
        }
        return AuthenticationUtil.runAsSystem(()-> {
            return addToDatabase(TRACKING_INSERT_USER, statement -> {
                statement.setString(1, super.getTrackedUsername(authorityName));
                statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                statement.setString(3, type.name());
                JSONObject json = buildJson(authorityName, type);
                PGobject obj = new PGobject();
                obj.setType("json");
                if (json != null)
                    obj.setValue(json.toString());
                statement.setObject(4, obj);

                return true;
            });
        });

    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef,String nodeVersion, EventType type) {
        super.trackActivityOnNode(nodeRef,nodeVersion,type);
        return AuthenticationUtil.runAsSystem(()-> {
            String version;
            if(nodeVersion==null || nodeVersion.isEmpty() || nodeVersion.equals("-1")){
                version=NodeServiceHelper.getProperty(nodeRef,CCConstants.CM_PROP_VERSIONABLELABEL);
            }
            else{
                version=nodeVersion;
            }
            return addToDatabase(TRACKING_INSERT_NODE, statement -> {
                statement.setLong(1, (Long) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.SYS_PROP_NODE_DBID)));
                statement.setString(2, nodeRef.getId());
                statement.setString(3, version);
                statement.setString(4, super.getTrackedUsername(null));
                statement.setDate(5, new Date(System.currentTimeMillis()));
                statement.setString(6, type.name());
                JSONObject json = buildJson(nodeRef, type);
                PGobject obj = new PGobject();
                obj.setType("json");
                if (json != null)
                    obj.setValue(json.toString());
                statement.setObject(7, obj);

                return true;
            });
        });
    }

    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(NodeRef nodeRef, EventType type) {
        return null;
    }
    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(String authorityName, EventType type) {
        return null;
    }
    @Override
    public List<StatisticEntryNode> getNodeStatisics(java.util.Date dateFrom,java.util.Date dateTo){
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            statement = con.prepareStatement(TRACKING_STATISTICS_NODE);
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            java.sql.ResultSet resultSet = statement.executeQuery();
            List<StatisticEntryNode> result=new ArrayList<>();
            while(resultSet.next()) {
                StatisticEntryNode entry = new StatisticEntryNode();
                entry.setNode(resultSet.getString("node"));
                if(result.contains(entry)){
                    entry=result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")),resultSet.getInt("count"));
                result.add(entry);
            }
            return result;
        }catch(Throwable t){
            t.printStackTrace();
        }finally {
            dbAlf.cleanUp(con, statement);
        }
        return null;
    }
    private static boolean addToDatabase(String statementContent,FillStatement fillStatement){
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            statement = con.prepareStatement(statementContent);
            if(fillStatement.onFillStatement(statement)) {
                statement.executeUpdate();
            }
            statement.close();
            con.commit();

        }catch(Throwable t){
            t.printStackTrace();
        }finally {
            dbAlf.cleanUp(con, statement);
        }
        return true;
    }
    private interface FillStatement{
        boolean onFillStatement(PreparedStatement statement) throws SQLException;
    }
}
