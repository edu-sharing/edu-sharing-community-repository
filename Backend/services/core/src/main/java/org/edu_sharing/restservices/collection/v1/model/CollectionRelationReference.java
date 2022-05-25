package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.model.CollectionRef;

@Schema(description = "")
public class CollectionRelationReference extends Node {
    private CollectionRef.RelationType relationType;

    @JsonProperty
    public CollectionRef.RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(CollectionRef.RelationType relationType) {
        this.relationType = relationType;
    }
}
