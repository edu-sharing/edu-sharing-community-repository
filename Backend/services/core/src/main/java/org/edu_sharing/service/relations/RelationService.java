package org.edu_sharing.service.relations;

import org.edu_sharing.service.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;

public interface RelationService {
    @NotNull NodeRelation getRelations(@NotNull String node);


    void createRelation(
            @NotNull String fromNode,
            @NotNull String toNode,
            @NotNull InputRelationType relationType) throws NodeRelationException, InsufficientPermissionException;

    void deleteRelation(
            @NotNull String fromNode,
            @NotNull String toNode,
            @NotNull InputRelationType relationType) throws NodeRelationException, InsufficientPermissionException;

    void changeAuthority(@NotNull String actualAuthority, @NotNull String newAuthority);
}
