package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeUrls;


@Data
public class SignedNodeEntry {

    @JsonProperty(required = true)
    private Node node;
    @JsonProperty(required = true)
    private String jwt;
    @JsonProperty(required = true)
    private String signedNode;
    @JsonProperty(required = true)
    private String signature;
    @JsonProperty
    private String signatureAlgorithm;
    @JsonProperty
    private String renderingBaseUrl;
    @JsonProperty
    @Schema(description = "Precomputed URLs for the node, e.g. for LTI tool nodes, the LTI resource-link login initiation URL.")
    private NodeUrls nodeUrls;
}
