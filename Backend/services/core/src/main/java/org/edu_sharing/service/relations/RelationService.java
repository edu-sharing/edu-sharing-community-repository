package org.edu_sharing.service.relations;

import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;

public interface RelationService {

    /**
     * Retrieves a list of tracked relation data within the specified date range and subject to the specified limits.
     * This method is restricted to administrator access.
     *
     * @param from the start date for filtering tracked data, must not be null
     * @param to the end date for filtering tracked data, can be null to include all dates after 'from'
     * @param limit the restriction on the number of results or performance constraints, can be null for no limit
     * @return a list of {@code RelationData} entries matching the specified criteria
     */
    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<RelationData> getTrackedData(@NotNull Date from, Date to, Limit limit);

    /**
     * Retrieves a list of deleted relation tracking data within the specified date range
     * and subject to the specified limits. Only accessible by administrators.
     *
     * @param from the start date to filter deleted tracked data, must not be null
     * @param to the end date to filter deleted tracked data, can be null to include all dates beyond 'from'
     * @param limit the limit for the number of results or performance constraints can be null for no limit
     * @return a list of deleted {@code RelationData} entries matching the specified criteria
     */
    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<RelationData> getDeletedTrackedData(@NotNull Date from, Date to, Limit limit);

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
