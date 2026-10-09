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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ScopeBulkFailure")
    public static class Failure implements Serializable {
        private String nodeId;
        private String message;
    }
}
