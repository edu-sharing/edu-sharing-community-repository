package org.edu_sharing.service.relations;

import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;

public interface RelationService {

    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<RelationData> getTrackedData(Date from, Limit limit);

    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<RelationData> getDeletedTrackedData(Date from, Limit limit);

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
