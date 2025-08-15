package org.edu_sharing.service.share.ibatis;

import org.alfresco.repo.webdav.LockInfoImpl;
import org.apache.ibatis.annotations.*;
import org.edu_sharing.service.share.ShareInfoData;
import org.edu_sharing.service.share.ShareStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Mapper
public interface ShareInfoMapper {

    @Insert("INSERT INTO edu_share_info(node_id, shared_by, shared_with, share_status, share_type, timestamp) VALUES (#{nodeId}, #{sharedBy}, #{sharedWith}, #{shareStatus}, #{shareType}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void create(ShareInfoData shareInfoData);

    @Insert("<script>INSERT INTO edu_share_info(node_id, shared_by, shared_with, share_status, share_type, timestamp) VALUES <foreach collection='shareInfoDatas' item='item' separator=','> (#{item.nodeId}, #{item.sharedBy}, #{item.sharedWith}, #{item.shareStatus}, #{item.shareType}, #{item.timestamp})</foreach> </script>")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void createAll(List<ShareInfoData> shareInfoDatas);

    @Delete("DELETE FROM edu_share_info WHERE id = #{id}")
    void delete(ShareInfoData shareInfoData);

    @Select("<script>DELETE FROM edu_share_info WHERE id IN <foreach item='item' collection='shareIds' open='(' separator=',' close=')'>#{item}</foreach></script>")
    List<Long> deleteAll(List<Long> shareIds);

    @Select("DELETE FROM edu_share_info WHERE (shared_with = #{sharedWith} OR shared_by = #{sharedBy}) AND share_status = #{shareStatus} RETURNING id")
    List<Long> deleteBySharedWithOrSharedByAndShareStatus(String sharedWith, String sharedBy, ShareStatus shareStatus);

    @Select("DELETE FROM edu_share_info WHERE node_id = #{nodeId} RETURNING id")
    List<Long> deleteByNodeId(String nodeId);

    @Select("SELECT * FROM edu_share_info WHERE timestamp >= #{after} ORDER BY timestamp DESC LIMIT #{limit} OFFSET #{skip}")
    @Results({
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "shared_by", property = "sharedBy"),
            @Result(column = "shared_with", property = "sharedWith"),
            @Result(column = "share_status", property = "shareStatus"),
            @Result(column = "share_type", property = "shareType"),
    })
    @NotNull List<ShareInfoData> getDataForAllUsers(@NotNull Date after, long skip, int limit);

    @Select("SELECT * FROM edu_share_info WHERE shared_with LIKE '%' || #{username} || '%' AND timestamp >= #{after} ORDER BY timestamp DESC")
    @Results({
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "shared_by", property = "sharedBy"),
            @Result(column = "shared_with", property = "sharedWith"),
            @Result(column = "share_status", property = "shareStatus"),
            @Result(column = "share_type", property = "shareType"),
    })
    @NotNull List<ShareInfoData> getDataForUser(@NotNull String username, @NotNull Date after);

    @Select("SELECT COUNT(*) FROM edu_share_info")
    long count();

    @Select("SELECT * FROM edu_share_info WHERE node_id = #{nodeId}")
    @Results({
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "shared_by", property = "sharedBy"),
            @Result(column = "shared_with", property = "sharedWith"),
            @Result(column = "share_status", property = "shareStatus"),
            @Result(column = "share_type", property = "shareType"),
    })
    List<ShareInfoData> getAllSharesByNodeId(String nodeId);

    @Select("<script> DELETE FROM edu_share_info WHERE node_id = #{nodeId} AND shared_by = #{sharedBy} AND share_status = #{shareStatus} AND shared_with IN <foreach item='item' collection='sharedWiths' open='(' separator=',' close=')'>#{item}</foreach> RETURNING id </script>")
    List<Long> deleteAllByNodeIdAndSharedByAndShareStatusAndSharedWithIn(String nodeId, String sharedBy, ShareStatus shareStatus, Collection<String> sharedWiths);

    @Select("<script> SELECT * FROM edu_share_info WHERE id IN <foreach item='item' collection='ids' open='(' separator=',' close=')'>#{item}</foreach></script>")
    @Results({
            @Result(column = "node_id", property = "nodeId"),
            @Result(column = "shared_by", property = "sharedBy"),
            @Result(column = "shared_with", property = "sharedWith"),
            @Result(column = "share_status", property = "shareStatus"),
            @Result(column = "share_type", property = "shareType"),
    })
    List<ShareInfoData> getAllSharesByIdIn(Collection<Long> ids);

}
