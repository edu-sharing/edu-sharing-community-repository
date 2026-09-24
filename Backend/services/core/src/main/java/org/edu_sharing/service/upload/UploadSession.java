package org.edu_sharing.service.upload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Public, client-facing view of a chunked upload session.
 */
@Data
public class UploadSession {

    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String nodeId;

    @JsonProperty(required = true)
    private long size;

    @JsonProperty(required = true)
    private long chunkSize;

    @JsonProperty(required = true)
    private long offset;

    @JsonProperty(required = true)
    private UploadSessionState state;

    /**
     * set when state is FAILED; shaped like {@code ErrorResponse} (exception class name, message,
     * details map e.g. actualSize/maxSize) so the same client-side error mapping used for the
     * single-request content upload can be reused.
     */
    private String errorCode;
    private String errorMessage;
    private Map<String, Serializable> errorDetails;
}
