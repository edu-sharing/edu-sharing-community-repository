package org.edu_sharing.restservices;

import org.edu_sharing.service.upload.UploadChunkOffsetMismatchException;

import java.io.Serializable;
import java.util.Map;

public class DAOUploadOffsetMismatchException extends DAOException {
    private final long offset;

    DAOUploadOffsetMismatchException(UploadChunkOffsetMismatchException cause, String nodeId) {
        super(cause, nodeId);
        this.offset = cause.getOffset();
    }

    @Override
    public Map<String, Serializable> getDetails() {
        return Map.of("offset", offset);
    }
}
