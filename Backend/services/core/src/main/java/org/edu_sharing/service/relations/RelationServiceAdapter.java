package org.edu_sharing.service.relations;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Limit;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RelationServiceAdapter implements RelationService {

    @NotNull
    @Override
    public List<RelationData> getRelations(@NotNull String node) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public RelationData createRelation(@NotNull CreateRelationRequest request) throws NodeRelationException {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public RelationData updateRelation(@NotNull UpdateRelationRequest request) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteRelation(@NotNull String fromNode, @NotNull String toNode, @NotNull InputRelationType relationType) throws NodeRelationException {
        throw new NotImplementedException();
    }

    @Override
    public void changeAuthority(@NotNull String actualAuthority, @NotNull String newAuthority) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public RelationData approveRelation(@NotNull String fromNode, @NotNull String toNode, @NotNull InputRelationType relationType) {
        throw new NotImplementedException();
    }
}
