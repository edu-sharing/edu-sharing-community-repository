package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UploadInitRequest {

    @JsonProperty(required = true)
    private long size;

    /**
     * leave empty = no new version, otherwise a new version is generated (same semantics as the
     * versionComment query parameter of the single-request content upload endpoint)
     */
    private String versionComment;
}
