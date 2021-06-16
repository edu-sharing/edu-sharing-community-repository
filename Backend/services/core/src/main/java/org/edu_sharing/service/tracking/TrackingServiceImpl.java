package org.edu_sharing.service.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.tracking.ibatis.EduTrackingMapper;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.ibatis.NodeResult;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.postgresql.util.PSQLException;
import org.springframework.context.ApplicationContext;

import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrackingServiceImpl extends TrackingServiceDefault{
    private static final List<String> EXISTING_FIELDS = Arrays.asList("authority","authority_organization","authority_mediacenter");
    public static Logger logger = Logger.getLogger(TrackingServiceImpl.class);

    public static String TRACKING_NODE_TABLE_ID = "edu_tracking_node";
    public static String TRACKING_USER_TABLE_ID = "edu_tracking_user";

    public static String TRACKING_DELETE_NODE = "DELETE FROM " + TRACKING_NODE_TABLE_ID +" WHERE authority = ?";
    public static String TRACKING_DELETE_USER = "DELETE FROM " + TRACKING_USER_TABLE_ID +" WHERE authority = ?";

    public static String TRACKING_UPDATE_NODE = "UPDATE " + TRACKING_NODE_TABLE_ID +" SET authority = ? WHERE authority = ?";
    public static String TRACKING_UPDATE_USER = "UPDATE " + TRACKING_USER_TABLE_ID +" SET authority = ? WHERE authority = ?";

    public static String TRACKING_INSERT_NODE = "insert into " + TRACKING_NODE_TABLE_ID +" (node_id,node_uuid,original_node_uuid,node_version,authority,authority_organization,authority_mediacenter,time,type,data) VALUES (?,?,?,?,?,?,?,?,?,?)";
    public static String TRACKING_INSERT_USER = "insert into " + TRACKING_USER_TABLE_ID +" (authority,authority_organization,authority_mediacenter,time,type,data) VALUES (?,?,?,?,?,?)";
    public static String TRACKING_STATISTICS_CUSTOM_GROUPING = "SELECT type,COUNT(*) :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type :grouping" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_USER = "SELECT authority,authority_organization,authority_mediacenter,time as date,type,COUNT(*) :fields from edu_tracking_user as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY authority,authority_organization,authority_mediacenter,time,type :grouping" +
            " ORDER BY date" +
            " LIMIT 100";
    public static String TRACKING_STATISTICS_NODE = "SELECT node_uuid as node,authority,authority_organization,authority_mediacenter,time as date,type,COUNT(*) :fields from edu_tracking_node as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY node,authority,authority_organization,authority_mediacenter,time,type :grouping" +
            " ORDER BY date" +
            " LIMIT 100";
    public static String TRACKING_STATISTICS_NODE_GROUPED = "SELECT node_uuid as node,type,COUNT(*) :fields from edu_tracking_node as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY node,type :grouping" +
            " ORDER BY count DESC" +
            " LIMIT 300";
    public static String TRACKING_STATISTICS_NODE_SINGLE = "SELECT type,COUNT(*) from edu_tracking_node as tracking" +
            " WHERE node_uuid = ? AND time BETWEEN ? AND ?" +
            " GROUP BY type" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_DAILY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm-dd') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_MONTHLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_YEARLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    private final NodeService nodeService;

    public TrackingServiceImpl() {
        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

        ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService=serviceRegistry.getNodeService();
        /*
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        PGPoolingDataSource source = new PGPoolingDataSource();
        source.setDataSourceName("tracking data source");
        source.setServerName("localhost");
        source.setDatabaseName("edu_tracking_node");
        source.setUser("testuser");
        source.setPassword("testpassword");
        source.setMaxConnections(10);

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("tracking", transactionFactory, source);
        Configuration configuration = new Configuration(environment);
        */
        try {
            new ConnectionDBAlfresco().getSqlSessionFactoryBean().getConfiguration().addMapper(EduTrackingMapper.class);
        }catch(BindingException ignored) {}
    }

    @Override
    public List<String> getAlteredNodes(java.util.Date from) {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
            return session.getMapper(EduTrackingMapper.class).eduAlteredNodes(from).stream().
                    map(NodeResult::getNodeid).collect(Collectors.toList());
        }
    }
    @Override
    public List<NodeData> getNodeData(String nodeId, java.util.Date from) {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
            return session.getMapper(EduTrackingMapper.class).
                    eduNodeData(nodeId, "YYYY-MM-DD", from);
        }
    }
    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        super.trackActivityOnUser(authorityName,type);
        if(authorityName==null || authorityName.equals(ApplicationInfoList.getHomeRepository().getGuest_username()) || authorityName.equals(AuthenticationUtil.getSystemUserName())){
            return false;
        }
        return AuthenticationUtil.runAs(()-> execDatabaseQuery(TRACKING_INSERT_USER, statement -> {
            statement.setString(1, super.getTrackedUsername(authorityName));
            try {
                statement.setArray(2,statement.getConnection().createArrayOf("VARCHAR",SearchServiceFactory.getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray()));
            } catch (Exception e) {
                statement.setArray(2, null);
                logger.info("Failed to track organizations of user",e);
            }
            try {
                statement.setArray(3,statement.getConnection().createArrayOf("VARCHAR",SearchServiceFactory.getLocalService().getAllMediacenters().toArray()));
            } catch (Exception e) {
                statement.setArray(3, null);
                logger.info("Failed to track mediacenter of user",e);
            }
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            statement.setString(5, type.name());
            JSONObject json = buildJson(authorityName, type);
            PGobject obj = new PGobject();
            obj.setType("json");
            if (json != null)
                obj.setValue(json.toString());
            statement.setObject(6, obj);

            return true;
        }),authorityName);

    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef,NodeTrackingDetails details, EventType type) {
        super.trackActivityOnNode(nodeRef,details,type);
        
            String version;
            String nodeVersion = details==null ? null : details.getNodeVersion();
            if(nodeVersion==null || nodeVersion.isEmpty() || nodeVersion.equals("-1")){
                version=NodeServiceHelper.getProperty(nodeRef,CCConstants.CM_PROP_VERSIONABLELABEL);
            }
            else{
                version=nodeVersion;
            }
            String originalNodeRef = null;
            try {
                if (NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
                    originalNodeRef = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_IO_ORIGINAL);
                }
            }catch(Throwable ignored) { }
        String finalOriginalNodeRef = originalNodeRef;
        return execDatabaseQuery(TRACKING_INSERT_NODE, statement -> {
                statement.setLong(1, (Long) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.SYS_PROP_NODE_DBID)));
                statement.setString(2, nodeRef.getId());
                statement.setString(3, finalOriginalNodeRef);
                statement.setString(4, version);
                statement.setString(5, super.getTrackedUsername(null));
                try {
                    statement.setArray(6,statement.getConnection().createArrayOf("VARCHAR",SearchServiceFactory.getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray()));
                } catch (Exception e) {
                    logger.info("Failed to track organizations of user",e);
                }
                try {
                    statement.setArray(7,statement.getConnection().createArrayOf("VARCHAR",SearchServiceFactory.getLocalService().getAllMediacenters().toArray()));
                } catch (Exception e) {
                    logger.info("Failed to track mediacenter of user",e);
                }
                statement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
                statement.setString(9, type.name());
                JSONObject json = buildJson(nodeRef, details, type);
                PGobject obj = new PGobject();
                obj.setType("json");
                if (json != null)
                    obj.setValue(json.toString());
                statement.setObject(10, obj);

                return true;
            });
        
    }

    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(NodeRef nodeRef, NodeTrackingDetails details, EventType type) {
        /*
        try {
            // sample object for testing purposes
            return new JSONObject().put("ref",nodeRef.getId()).put("boolean",true).put("int",1).put("string","text").put("double",1.0);
        } catch (JSONException e) {}
        */
        return null;
    }
    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(String authorityName, EventType type) {
        /*
        try {
            // sample object for testing purposes
            return new JSONObject().put("school",authorityName.substring(0,1)).put("role",authorityName.substring(0,1));
        } catch (JSONException e) {}
        */
        return null;
    }
    @Override
    public List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntry> result = initList(StatisticEntry.class,type,dateFrom,dateTo);
            String query = getQuery(type, "edu_tracking_user", mediacenter, additionalFields, groupFields, filters);
            statement = con.prepareStatement(query);
            int index=1;
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if(mediacenter!=null && !mediacenter.isEmpty()){
                statement.setString(index++,mediacenter);
            }
            if (filters != null && !filters.isEmpty()) {
                for (String value : filters.values()) {
                    statement.setString(index++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntry entry = new StatisticEntry();
                boolean grouping=!type.equals(GroupingType.None);
                mapResult(EventType.valueOf(resultSet.getString("type")), additionalFields, groupFields, resultSet, entry);

                if (result.contains(entry) && grouping) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry) || !grouping) {
                    result.add(entry);
                }
            }
            Collections.sort(result);
            return result;
        }catch(Throwable t){
            throw t;
        }
        finally{
            dbAlf.cleanUp(con, statement);
        }
    }

    private void mapResult(EventType type, List<String> additionalFields, List<String> groupFields, ResultSet resultSet, StatisticEntry entry) throws SQLException {
        setAuthorityFromResult(resultSet, entry);
        if (additionalFields != null && additionalFields.size() > 0) {
            for (String field : additionalFields) {
                Map<String, Map<String, Long>> current = entry.getGroups().get(type);
                if(current==null) {
                    current = new HashMap<>();
                }
                // the sql field will add each property to an array like 1,2,1,3
                // we will map it to {1:2,2:1,3:1}
                String[] array = (String[]) resultSet.getArray(field).getArray();
                HashMap<String, Long> counted = new HashMap<>(Arrays.stream(array).map((a)->a==null ? "" : a)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
                current.put(field,counted);
                entry.getGroups().put(type, current);
            }
        }
        try{
            entry.setDate(resultSet.getString("date"));
        }catch(PSQLException e){
            // ignore, some queries don't have a date field
        }
        if(groupFields!=null && !groupFields.isEmpty()) {
            for(String field :groupFields) {
                entry.getFields().put(field, resultSet.getString(field));
            }
        }
    }

    @Override
    public List<StatisticEntryNode> getNodeStatisics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntryNode> result = initList(StatisticEntryNode.class,type,dateFrom,dateTo);
            String query = getQuery(type, "edu_tracking_node", mediacenter, additionalFields, groupFields, filters);
            statement=con.prepareStatement(query);
            int index=1;
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if(mediacenter!=null && !mediacenter.isEmpty()){
                statement.setString(index++,mediacenter);
            }
            if (filters != null && !filters.isEmpty()) {
                for (String value : filters.values()) {
                    statement.setString(index++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntryNode entry = new StatisticEntryNode();
                boolean grouping=!type.equals(GroupingType.None) || groupFields!=null && !groupFields.isEmpty();
                if(type.equals(GroupingType.Node)){
                    entry.setNode(resultSet.getString("node"));
                }
                else {
                    if (!grouping) {
                        entry.setNode(resultSet.getString("node"));
                    }
                }

                mapResult(EventType.valueOf(resultSet.getString("type")), additionalFields, groupFields, resultSet, entry);

                if (result.contains(entry) && grouping) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry) || !grouping) {
                    result.add(entry);
                }
            }
            Collections.sort(result);
            return result;
        }catch(Throwable t){
            throw t;
        }finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    /**
     * fechtes the counts for each event type for the given node, while the date must be between the given dates
     * If both dates are null, it will take the values across all dates
     * @return
     * @throws Throwable
     */
    @Override
    public StatisticEntry getSingleNodeData(NodeRef node,java.util.Date dateFrom,java.util.Date dateTo) throws Throwable{
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            String query = TRACKING_STATISTICS_NODE_SINGLE;
            statement=con.prepareStatement(query);
            int index=1;
            statement.setString(index++,node.getId());
            if(dateFrom==null)
                dateFrom = new java.util.Date(0);
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if(dateTo==null)
                dateTo = new java.util.Date();
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            ResultSet resultSet = statement.executeQuery();
            StatisticEntry entry = new StatisticEntry();
            while (resultSet.next()) {
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
            }
            return entry;
        }catch(Throwable t){
            throw t;
        }finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    @Override
    public void deleteUserData(String username) throws Throwable {
        if(getUserTrackingMode().equals(UserTrackingMode.none)){
            logger.info("User tracking is set to none, deleteUserData will do nothing");
            return;
        }
        execDatabaseQuery(TRACKING_DELETE_NODE,(statement)->{
            statement.setString(1,getTrackedUsername(username));
            return true;
        });
        execDatabaseQuery(TRACKING_DELETE_USER,(statement)->{
            statement.setString(1,getTrackedUsername(username));
            return true;
        });
    }

    @Override
    public void reassignUserData(String oldUsername, String newUsername) {
        if(getUserTrackingMode().equals(UserTrackingMode.none)){
            logger.info("User tracking is set to none, reassignUserData will do nothing");
            return;
        }
        execDatabaseQuery(TRACKING_UPDATE_NODE,(statement)->{
            statement.setString(1,getTrackedUsername(newUsername));
            statement.setString(2,getTrackedUsername(oldUsername));
            return true;
        });
        execDatabaseQuery(TRACKING_UPDATE_USER,(statement)->{
            statement.setString(1,getTrackedUsername(newUsername));
            statement.setString(2,getTrackedUsername(oldUsername));
            return true;
        });
    }

    private void setAuthorityFromResult(ResultSet resultSet, StatisticEntry entry) throws SQLException {
        try {
            entry.getAuthorityInfo().setAuthority(resultSet.getString("authority"));
        }catch(PSQLException e){}
        try {
            Array orgs = resultSet.getArray("authority_organization");
            if (orgs != null) {
                entry.getAuthorityInfo().setOrganizations((String[]) orgs.getArray());
            }
        }catch(PSQLException e){}
        try{
            Array mediacenters = resultSet.getArray("authority_mediacenter");
            if (mediacenters != null) {
                entry.getAuthorityInfo().setMediacenters((String[]) mediacenters.getArray());
            }
        }catch(PSQLException e){}
    }

    private <T extends StatisticEntry> List<T> initList(Class<T> clz, GroupingType type, java.util.Date dateFrom, java.util.Date dateTo) throws IllegalAccessException, InstantiationException {
        long DAY_DURATION=1000*60*60*24;

        List<T> list = new ArrayList<>();
        if(type.equals(GroupingType.None) || type.equals(GroupingType.Node))
            return list;

        Date date=new Date(dateFrom.getTime());
        String pattern;
        if(type.equals(GroupingType.Daily)){
            pattern="yyyy-MM-dd";
        }
        else if(type.equals(GroupingType.Monthly)){
            pattern="yyyy-MM";
        }
        else{
            pattern="yyyy";
        }
        SimpleDateFormat dt = new SimpleDateFormat(pattern);
        while(date.getTime()<dateTo.getTime()-DAY_DURATION){
            T obj=clz.newInstance();
            obj.setDate(dt.format(date));
            if(!list.contains(obj))
                list.add(obj);
            date.setTime(date.getTime()+DAY_DURATION);
        }
        return list;
    }

    private String getQuery(GroupingType type, String table, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws SQLException {
        try {
            String prepared=null;
            if (type.equals(GroupingType.Daily))
                prepared = TRACKING_STATISTICS_DAILY;
            else if (type.equals(GroupingType.Monthly))
                prepared = TRACKING_STATISTICS_MONTHLY;
            else if (type.equals(GroupingType.Yearly))
                prepared = TRACKING_STATISTICS_YEARLY;
            else if(type.equals(GroupingType.Node)){
                prepared = TRACKING_STATISTICS_NODE_GROUPED;
            }
            else if (type.equals(GroupingType.None)) {
                if(groupFields!=null && !groupFields.isEmpty()){
                    prepared = TRACKING_STATISTICS_CUSTOM_GROUPING;
                }
                else if (table.equals("edu_tracking_node")) {
                    prepared = TRACKING_STATISTICS_NODE;
                } else if (table.equals("edu_tracking_user")) {
                    prepared = TRACKING_STATISTICS_USER;
                }
            }
            if(prepared==null)
                throw new IllegalArgumentException("No statement found for tracking table " + table + " and mode " + type);

            prepared = prepared.replace(":table", table);

            StringBuilder filter = new StringBuilder("true");
            StringBuilder grouping = new StringBuilder();
            StringBuilder fields = new StringBuilder();
            if(mediacenter !=null && !mediacenter.isEmpty()){
                filter.append(" AND (ARRAY[?] <@ authority_mediacenter)");
            }
            if (filters != null && !filters.isEmpty()) {
                filter.append(" AND (");
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    filter.append(" AND ");
                    filter.append(makeDbField(entry.getKey(),false)).append(" = ?");
                }
                filter.append(")");
            }
            if (groupFields != null && !groupFields.isEmpty()) {
                for (String field : groupFields) {
                    grouping.append(",").append(makeDbField(field,false));
                    fields.append(",").append(makeDbField(field,false) + " as " + field);
                }
            }
            if (additionalFields != null && additionalFields.size() > 0) {
                for (String field : additionalFields) {
                    fields.append(",ARRAY_AGG(").append(makeDbField(field,true) + ") as " + field);
                    // if additional fields and no grouping is provided, add them to grouping otherwise there will be a postgres exception when fetching
                    // nope: it is now using ARRAY_AGG
                    //if(groupFields==null || groupFields.isEmpty()){
                    //    grouping.append(",").append(makeDbField(field));
                    //}
                }
            }
            prepared = prepared.replace(":fields", fields).replace(":filter", filter).replace(":grouping", grouping);
            logger.info(prepared);
            return prepared;
        }catch(Throwable t) {
            logger.error(t.getMessage(), t);
            throw t;
        }
    }

    private String makeDbField(String field,boolean useFirstIndex) {
        if(EXISTING_FIELDS.contains(field)){
            if(useFirstIndex)
                return field+"[1]";
            return field;
        }
        else if(field.toLowerCase().matches("[a-z]*[0-9]*")){
            return "data ->> '"+field+"'";
        }

        else
            throw new IllegalArgumentException("Fields for filter and grouping should only contain numbers and letters");
    }

    private static boolean execDatabaseQuery(String statementContent, FillStatement fillStatement){
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
            logger.error("Error tracking to database",t);
        }finally {
            dbAlf.cleanUp(con, statement);
        }
        return true;
    }
    private interface FillStatement{
        boolean onFillStatement(PreparedStatement statement) throws SQLException;
    }
}
