package org.edu_sharing.service.upload;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Internal, persisted state of a chunked upload session (stored as meta.json next to the chunk
 * data on the shared content store volume, so it survives pod restarts and is visible to every
 * repository replica).
 */
@Data
class UploadSessionMeta {
    private String id;
    private String owner;
    private String nodeId;
    private String versionComment;
    private long size;
    private long chunkSize;
    private UploadSessionState state;
    private String errorCode;
    private String errorMessage;
    private Map<String, Serializable> errorDetails;
    private long created;
    private long updated;
    private long processingHeartbeat;
}
