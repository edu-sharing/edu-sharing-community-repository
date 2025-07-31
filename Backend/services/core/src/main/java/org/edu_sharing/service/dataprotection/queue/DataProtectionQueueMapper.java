package org.edu_sharing.service.dataprotection.queue;

import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface DataProtectionQueueMapper {

    @Select("SELECT * FROM edu_dataprotection_queue")
    List<DataProtectionQueueEntry> findAll();

    @Select("SELECT * FROM edu_dataprotection_queue WHERE status = #{status}")
    List<DataProtectionQueueEntry> findAllByStatus(@Param("status") String status);


    @Select("SELECT * FROM edu_dataprotection_queue WHERE \"user\" = #{user}")
    DataProtectionQueueEntry findByUser(@Param("user") String user);

    @Insert("INSERT INTO edu_dataprotection_queue(\"user\",status,requested) VALUES(#{user},#{status},#{requested})")
    void insert(DataProtectionQueueEntry dataProtectionQueueEntry);

    @Update("UPDATE edu_dataprotection_queue SET status=#{status}, requested=#{requested}, node_id=#{node_id}, finished=#{finished} WHERE \"user\"=#{user}")
    void update(DataProtectionQueueEntry dataProtectionQueueEntry);

    @Delete("DELETE FROM edu_dataprotection_queue WHERE \"user\"=#{user}")
    void delete(DataProtectionQueueEntry dataProtectionQueueEntry);



    /**
     * @Insert("INSERT INTO edu_timed_node_permission(node_id,authority,\"user\",permission,\"from\",\"to\", activated)VALUES(#{node_id},#{authority},#{user},#{permission},#{from},#{to}, #{activated}) ON CONFLICT (node_id, authority, permission) DO UPDATE SET \"from\" = #{from}, \"to\" = #{to}, \"user\" = #{user}, activated = #{activated};")
     *     void save(TimedPermission timedPermission);
     *
     *     @Delete("DELETE FROM edu_timed_node_permission WHERE node_id = #{node_id} AND authority = #{authority} AND permission = #{permission}")
     *     void delete(TimedPermission permission);
     */

}
