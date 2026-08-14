package org.edu_sharing.service.share.ibatis;

import org.apache.ibatis.annotations.*;
import org.edu_sharing.service.share.ShareInfoOplogData;

import java.util.Date;
import java.util.List;

@Mapper
public interface ShareInfoOpLogMapper {

    /**
     * A row's id/timestamp is assigned when its INSERT runs, not when its transaction commits. So
     * under concurrent writes, a slower transaction can commit a row with an earlier (timestamp,
     * id) after a faster, later-started transaction has already committed and been picked up by a
     * poller - once that happens, the earlier row can never be reached again via a plain
     * (timestamp, id) cursor. This expression computes a safe upper bound instead of trusting
     * "now": the start time of the oldest transaction still active on this connection's database
     * (any transaction that begun before that point has definitely committed or aborted by now,
     * so nothing it might still write can land before this bound), minus a small fixed buffer for
     * clock/latency slop. With no long-running concurrent transactions this is ~now, so callers see
     * fresh data within a couple of seconds; a genuinely slow transaction extends it for exactly as
     * long as it stays open - never a fixed, blanket delay for everyone.
     *
     * <p>TODO: pg_stat_activity only reflects activity visible on the connection this query runs
     * on. If reads for this query are ever routed through a read replica (currently they are not,
     * as far as this codebase shows), this watermark would miss in-flight transactions on the
     * primary and the safety guarantee would silently break. Must keep this query pinned to the
     * primary connection, or replace it with a replica-safe mechanism (e.g. checking replica lag)
     * if that topology changes.
     */
    String SAFE_OPLOG_WATERMARK =
            "COALESCE(" +
            "  (SELECT MIN(xact_start) FROM pg_stat_activity WHERE state != 'idle' AND pid != pg_backend_pid() AND datname = current_database())," +
            "  now()" +
            ") AT TIME ZONE 'UTC' - INTERVAL '2 seconds'";

    @Insert("INSERT INTO edu_share_info_oplog(share_id, action, timestamp) VALUES (#{shareId}, #{action}, #{timestamp})")
    void create(ShareInfoOplogData shareInfoOplogData);

    @Insert("<script> INSERT INTO edu_share_info_oplog(share_id, action, timestamp) VALUES <foreach collection='shareInfoOplogData' item='item' separator=','> (#{item.shareId}, #{item.action}, #{item.timestamp})</foreach> </script>")
    void createAll(List<ShareInfoOplogData> shareInfoOplogData);

    @Select("SELECT * FROM edu_share_info_oplog WHERE id > #{id} ORDER BY id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAllAfterId(Long id, int limit);

    /**
     * afterId is a tiebreaker for rows sharing the exact same timestamp (common for bulk share
     * operations), so callers can page through a timestamp range via a (timestamp, id) cursor
     * without ever skipping or re-fetching a row. See {@link #SAFE_OPLOG_WATERMARK} for why the
     * upper bound is never just "now".
     */
    @Select("SELECT * FROM edu_share_info_oplog WHERE (timestamp, id) > (#{timestamp}, #{afterId}) AND timestamp <= " + SAFE_OPLOG_WATERMARK + " ORDER BY timestamp, id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAllAfterTimestamp(Date timestamp, long afterId, int limit);

    @Select("SELECT * FROM edu_share_info_oplog ORDER BY id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAll(int limit);

    @Select("SELECT COUNT(*) FROM edu_share_info_oplog")
    long count();

    /**
     * afterId is a tiebreaker for rows sharing the exact same timestamp (common for bulk share
     * operations), so callers can page through a timestamp range via a (timestamp, id) cursor
     * without ever skipping or re-fetching a row. See {@link #SAFE_OPLOG_WATERMARK} for why the
     * upper bound is never just "until" as passed by the caller.
     */
    @Select("SELECT * FROM edu_share_info_oplog WHERE (timestamp, id) > (#{after}, #{afterId}) AND timestamp <= LEAST(#{until}, " + SAFE_OPLOG_WATERMARK + ") ORDER BY timestamp, id FETCH NEXT #{limit} ROWS ONLY")
    @Results({
            @Result(column = "share_id", property = "shareId")
    })
    List<ShareInfoOplogData> getAllBetweenTimestamp(Date after, long afterId, Date until, int limit);
}
