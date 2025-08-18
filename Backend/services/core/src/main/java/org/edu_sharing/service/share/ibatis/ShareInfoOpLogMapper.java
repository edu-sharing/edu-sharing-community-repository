package org.edu_sharing.service.share.ibatis;

import org.apache.ibatis.annotations.*;
import org.edu_sharing.service.share.ShareInfoOplogData;

import java.util.Date;
import java.util.List;

@Mapper
public interface ShareInfoOpLogMapper {

    @Insert("INSERT INTO edu_share_info_oplog(share_id, action, timestamp) VALUES (#{shareId}, #{action}, #{timestamp})")
    void create(ShareInfoOplogData shareInfoOplogData);

    @Insert("<script> INSERT INTO edu_share_info_oplog(share_id, action, timestamp) VALUES <foreach collection='shareInfoOplogData' item='item' separator=','> (#{item.shareId}, #{item.action}, #{item.timestamp})</foreach> </script>")
    void createAll(List<ShareInfoOplogData> shareInfoOplogData);

    @Select("SELECT * FROM edu_share_info_oplog WHERE id > #{id} ORDER BY id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAllAfterId(Long id, int limit);

    @Select("SELECT * FROM edu_share_info_oplog WHERE timestamp > #{timestamp} ORDER BY timestamp FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAllAfterTimestamp(Date timestamp, int limit);

    @Select("SELECT * FROM edu_share_info_oplog ORDER BY id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAll(int limit);

    @Select("SELECT COUNT(*) FROM edu_share_info_oplog")
    long count();

}
