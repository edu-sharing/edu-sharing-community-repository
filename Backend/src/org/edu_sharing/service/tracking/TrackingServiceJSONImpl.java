package org.edu_sharing.service.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringEscapeUtils;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TrackingServiceJSONImpl extends TrackingServiceDefault{
    public static String TRACKING_TABLE_ID = "EDU_TRACKING";
    public static String TRACKING_TABLE_ATT_ID = "JSON";
    public static String TRACKING_INSERT = "insert into " + TRACKING_TABLE_ID +" (" + TRACKING_TABLE_ATT_ID + ") VALUES (?)";

    public TrackingServiceJSONImpl() {
    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef, EventType type) {
        super.trackActivityOnNode(nodeRef,type);
        addToDatabase(buildJson(nodeRef,type));
        return true;
    }

    private JSONObject buildJson(NodeRef nodeRef, EventType type) {
        try {
            JSONObject json = new JSONObject();
            json.put("authority", AuthenticationUtil.getFullyAuthenticatedUser());
            JSONObject node = new JSONObject();
            node.put("identifier",nodeRef.getStoreRef().getIdentifier());
            node.put("protocol",nodeRef.getStoreRef().getProtocol());
            node.put("id",nodeRef.getId());
            json.put("node",node);
            json.put("type",type.toString());
            return json;
        }catch(Throwable t){
            t.printStackTrace();
            return null;
        }

    }

    private static void addToDatabase(JSONObject json){
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            statement = con.prepareStatement(TRACKING_INSERT);
            String jsonData = StringEscapeUtils.escapeSql(json.toString());
            statement.setString(1, jsonData);
            statement.executeUpdate();
            con.commit();

        }catch(Throwable t){
            t.printStackTrace();
        }finally {
            dbAlf.cleanUp(con, statement);
        }
    }
}
