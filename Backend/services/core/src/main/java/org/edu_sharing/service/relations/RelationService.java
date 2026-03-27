package org.edu_sharing.service.relations;

import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RelationService {

    @NotNull List<RelationData> getRelations(@NotNull String node);

    @NotNull RelationData createRelation(
            @NotNull CreateRelationRequest request);
    @NotNull RelationData updateRelation(@NotNull UpdateRelationRequest request);

    void deleteRelation(
            @NotNull String fromNode,
            @NotNull String toNode,
            @NotNull InputRelationType relationType);

    void changeAuthority(@NotNull String actualAuthority, @NotNull String newAuthority);

    @NotNull RelationData approveRelation(
            @NotNull String fromNode,
            @NotNull String toNode,
            @NotNull InputRelationType relationType);

}
