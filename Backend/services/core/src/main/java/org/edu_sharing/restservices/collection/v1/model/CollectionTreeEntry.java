package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A created collection together with its created children (mirrors the request tree).
 */
@Data
public class CollectionTreeEntry implements Serializable {

    @JsonProperty(required = true)
    private Node collection;

    @JsonProperty(required = true)
    private List<CollectionTreeEntry> children = new ArrayList<>();

    @Schema(description = "Materials (references) of this collection. Only filled if requested (includeReferences=true)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CollectionReference> references;
}
