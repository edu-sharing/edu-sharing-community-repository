package org.edu_sharing.service.scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * State of a bulk job (mongo collection scope_bulk_job).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeBulkStatus implements Serializable {
    public enum State {
        QUEUED,
        RUNNING,
        FINISHED,
        FAILED
    }

    private String jobId;
    private String scopeId;
    private State state;
    private long total;
    private long done;
    /** nodes that could not be changed, the job continues on errors */
    private List<Failure> failed;
    private String startedBy;
    private Date startedAt;
    private Date finishedAt;
    /**
     * queued jobs only: the jobs that run before this one (bulk jobs run one after another) and their progress, without
     * details of these jobs (they may belong to other scopes)
     */
    private Queue queue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ScopeBulkQueue")
    public static class Queue implements Serializable {
        /** number of queued or running jobs before this one */
        private long jobsAhead;
        /** nodes the jobs ahead already changed / will change (0 while they are not started) */
        private long done;
        private long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ScopeBulkFailure")
    public static class Failure implements Serializable {
        private String nodeId;
        private String message;
    }
}
