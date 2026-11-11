package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One collection inside a tree which is created in a single request.
 */
@Data
public class CollectionTreeNode implements Serializable {

    @Schema(description = "Title of the collection", required = true)
    @JsonProperty(required = true)
    private String title;

    @Schema(description = "Description of the collection")
    private String description;

    @Schema(description = "Color of the collection (e.g. #FF0000)")
    private String color;

    @Schema(description = "Type of the collection, defaults to \"default\"")
    private String type;

    @Schema(description = "Scope of the collection")
    private String scope;

    @Schema(description = "Additional metadata properties which are set on the new collection (same format as PUT /node/v1/nodes/{repository}/{node}/metadata)")
    private Map<String, String[]> properties;

    @Schema(description = "Child collections, created below this collection")
    private List<CollectionTreeNode> children = new ArrayList<>();
}
