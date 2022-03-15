package org.edu_sharing.service.relations;

import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class RelationServiceAdadapter implements RelationService {

    public RelationServiceAdadapter(String appId) {

    }

    @NotNull
    @Override
    public NodeRelation getRelations(@NotNull String node) {
        return new NodeRelation(node);
    }

    @Override
    public void createRelation(@NotNull String fromNode, @NotNull String toNode, @NotNull InputRelationType relationType) throws NodeRelationException {
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
}
