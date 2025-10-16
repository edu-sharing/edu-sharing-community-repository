package org.edu_sharing.repository.server.jobs.ibatis;

import org.apache.ibatis.annotations.*;
import org.edu_sharing.repository.server.jobs.JobQueueEntry;

import java.util.List;

@Mapper
public interface JobQueueMapper {

    @Insert("INSERT INTO edu_job_queue (is_unique,job_group,requested,last_updated,ttl,status,bean,method,param_types,params,user_name,job_hash) VALUES (#{unique},#{group},#{requested},#{lastUpdated},#{ttl},#{status},#{bean},#{method},#{paramTypes},#{params},#{user},#{jobHash})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void insert(JobQueueEntry entry);


    //TODO lastUpdated & timeoutDuration
    @Select("""
            UPDATE edu_job_queue as q SET status = 1, last_updated = CURRENT_TIMESTAMP
            WHERE q.id = (
                SELECT id FROM edu_job_queue s
                WHERE s.status = 0
                  AND NOT EXISTS (
                    SELECT 1
                    FROM edu_job_queue b
                    WHERE b.status = 1
                      AND b.job_group <> ''
                      AND b.job_group = s.job_group
                )
                ORDER BY s.requested
                LIMIT 1
            )
            RETURNING q.id as "id", q.is_unique as "unique", q.job_group as "group", q.requested as "requested", q.last_updated as "lastUpdated", q.status as "status", q.ttl as "ttl", q.job_hash as "jobHash", q.bean as "bean", q.method as "method", q.param_types as "paramTypes", q.params as "params", q.user_name as "user";
            """)
    JobQueueEntry getNext();


    @Update("UPDATE edu_job_queue SET status = #{status}, last_updated = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateStatus(JobQueueEntry entry);

    @Update("UPDATE edu_job_queue SET last_updated = #{lastUpdated} WHERE id = #{id}")
    void updateLastUpdated(JobQueueEntry entry);


    @Delete("DELETE FROM edu_job_queue WHERE id = #{id}")
    void delete(JobQueueEntry nextJob);

    @Select("""
            DELETE FROM edu_job_queue as q
            WHERE q.status = 1 AND q.last_updated notnull AND q.last_updated + q.ttl < now()
            RETURNING q.id as "id", q.is_unique as "unique", q.job_group as "group", q.requested as "requested", q.last_updated as "lastUpdated", q.status as "status", q.ttl as "ttl", q.job_hash as "jobHash", q.bean as "bean", q.method as "method", q.param_types as "paramTypes", q.params as "params", q.user_name as "user";
            """)
    JobQueueEntry[] deleteExpired();

    @Select("""
            SELECT q.id as "id", q.is_unique as "unique", q.job_group as "group", q.requested as "requested", q.last_updated as "lastUpdated", q.status as "status", q.ttl as "ttl", q.job_hash as "jobHash", q.bean as "bean", q.method as "method", q.param_types as "paramTypes", q.params as "params", q.user_name as "user"
            FROM edu_job_queue as q
            ORDER BY q.requested, q.id
            LIMIT #{limit}
            OFFSET #{skip}
            """)
    List<JobQueueEntry> getJobs(int skip, int limit);

    @Delete("<script>DELETE FROM edu_job_queue WHERE id IN <foreach item='item' collection='jobIds' open='(' separator=',' close=')'>#{item}</foreach></script>")
    void deleteByJobIds(List<Long> jobIds);

    @Update("UPDATE edu_job_queue SET status = 0, last_updated = null WHERE id = #{jobId}")
    void resetStatus(Long jobId);
}
