package org.edu_sharing.service.upload;

import java.io.InputStream;

/**
 * Stores a large node content upload as a sequence of small chunks on disk instead of a single
 * long-lived HTTP request, so it survives reverse-proxy send/read timeouts. Node-level permission
 * checks are the caller's responsibility (see {@code NodeDao#initUpload}); this service only
 * enforces that a session is only ever driven by the user that created it.
 * <p>
 * Session metadata and chunk data live on the shared content store volume so any repository pod
 * can serve any request for a given session.
 */
public interface ChunkedUploadService {

    /**
     * Starts a new upload session for {@code nodeId}. Rejects {@code size} early against
     * {@code security.fileManagement.limits.fileSize} so the client does not waste bandwidth
     * uploading a file that will be rejected anyway once written.
     */
    UploadSession initUpload(String nodeId, long size, String versionComment);

    /**
     * Appends the bytes read from {@code data} (until EOF) at {@code offset}. Throws
     * {@link UploadChunkOffsetMismatchException} if {@code offset} does not match the number of
     * bytes already stored, so the client can resync.
     */
    UploadSession appendChunk(String uploadId, String nodeId, long offset, InputStream data);

    UploadSession getSession(String uploadId, String nodeId);

    /**
     * Marks the session as PROCESSING and asynchronously hands the assembled file to
     * {@code writer}, so the request that triggers this returns immediately, well within any
     * reverse-proxy timeout. Poll {@link #getSession} for the outcome (DONE/FAILED).
     * Calling this again while already PROCESSING/DONE is a no-op that returns the current state.
     */
    UploadSession completeUpload(String uploadId, String nodeId, UploadContentWriter writer);

    void abortUpload(String uploadId, String nodeId);

    /**
     * Removes sessions inactive for longer than {@code ttlMillis} and fails PROCESSING sessions
     * whose processing heartbeat went stale (e.g. the pod handling them was killed). Safe to run
     * concurrently from every pod. Called by {@code UploadSessionCleanupJob}.
     */
    void cleanup(long ttlMillis);
}
