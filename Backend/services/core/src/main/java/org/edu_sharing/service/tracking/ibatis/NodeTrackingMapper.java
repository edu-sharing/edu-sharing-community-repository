package org.edu_sharing.service.tracking.ibatis;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface NodeTrackingMapper {

    @Insert("INSERT INTO edu_tracking_node (node_id,node_uuid,original_node_uuid,node_version,authority,authority_organization,authority_mediacenter,time,type,data,license, shared_with_mediacenters) VALUES (#{nodeId},#{node_uuid},#{original_node_uuid},#{node_version},#{authority},#{authority_organization},#{authority_mediacenter},#{time},#{type},#{data},#{license},#{shared_with_mediacenters})")
    void insertNode(NodeTrackingEntry node);

    @Update("UPDATE edu_tracking_node SET authority = #{newAuthority} WHERE authority = #{oldAuthority}")
    void updateNodesWithAuthority(String oldAuthority, String newAuthority);

    @Delete("DELETE FROM edu_tracking_node WHERE authority = #{authority}")
    void deleteNodesByAuthority(String authority);

    @Select("SELECT node_uuid AS nodeid FROM edu_tracking_node WHERE time = #{from} GROUP BY node_uuid")
    List<NodeResult> eduAlteredNodes(@Param("from") Date from);

    @Select("SELECT null AS timestamp, jsonb_object_agg(type, count)::TEXT AS counts FROM ("
            + "SELECT COUNT(*) AS count, type "
            + "FROM edu_tracking_node etn "
            + "WHERE node_uuid = #{id} "
            + "GROUP BY type "
            + ") AS sub "
            + "UNION "
            + "SELECT timestamp, jsonb_object_agg(type, count)::TEXT AS counts FROM ("
            + "SELECT TO_CHAR(time,#{format}) AS timestamp , COUNT(*) AS count, type "
            + "FROM edu_tracking_node etn "
            + "WHERE time >= #{from} AND node_uuid = #{id} GROUP BY timestamp, type"
            + ") AS sub "
            + "GROUP BY timestamp "
            + "ORDER BY timestamp")
    List<NodeData> eduNodeData(@Param("id") String id, @Param("format") String format, @Param("from") Date from);

}
