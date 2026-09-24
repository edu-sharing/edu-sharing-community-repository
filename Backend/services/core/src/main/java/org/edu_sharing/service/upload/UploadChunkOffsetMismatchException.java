package org.edu_sharing.service.upload;

import lombok.Getter;

/**
 * Thrown when a chunk is sent at an offset that does not match the number of bytes already
 * stored for the upload session, e.g. after a lost response caused the client to retry a chunk
 * that was in fact already persisted. The client is expected to resync using {@link #getOffset()}.
 */
@Getter
public class UploadChunkOffsetMismatchException extends RuntimeException {
    private final String uploadId;
    private final long offset;

    public UploadChunkOffsetMismatchException(String uploadId, long offset) {
        super("chunk offset mismatch for upload " + uploadId + ", expected offset " + offset);
        this.uploadId = uploadId;
        this.offset = offset;
    }
}
