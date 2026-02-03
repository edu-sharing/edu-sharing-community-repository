package org.edu_sharing.restservices;

import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.NodeRelationData;
import org.edu_sharing.restservices.shared.User;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.relations.*;
import org.edu_sharing.util.CheckedFunction;
import org.springframework.data.domain.Limit;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RelationDao {
    private final AuthorityService authorityService;
    private final RelationService relationService;
    private final NodeRelationTraceService nodeRelationTraceService;
    private final RepositoryDao repoDao;

    public RelationDao(RepositoryDao repoDao) {
        this.repoDao = repoDao;
        this.relationService = RelationServiceFactory.getInstance().getService(repoDao.getId());
        this.authorityService = AuthorityServiceFactory.getInstance().getService(repoDao.getId());
        this.nodeRelationTraceService = NodeRelationTraceServiceFactory.getInstance().getService(repoDao.getId());
    }

    public NodeRelationData createRelation(CreateRelationRequest request) {
        return mapRelationData(this.relationService.createRelation(request));
    }

    public void deleteRelation(String sourceNodeId, String targetNodeId, InputRelationType inputRelationType) {
        this.relationService.deleteRelation(sourceNodeId, targetNodeId, inputRelationType);
    }

    public NodeRelationData approveRelation(String sourceNodeId, String targetNodeId, InputRelationType relationType) {
        return mapRelationData(this.relationService.approveRelation(sourceNodeId, targetNodeId, relationType));
    }

    public List<NodeRelationData> getRelations(String sourceNodeId) {
        List<org.edu_sharing.service.relations.RelationData> relations = this.relationService.getRelations(sourceNodeId);
        return relations.stream().map(CheckedFunction.wrap(this::mapRelationData, null))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<NodeRelationData> traceRelations(String sourceNodeId, Integer maxDepth) {
        List<org.edu_sharing.service.relations.RelationData> relations = this.nodeRelationTraceService.traceRelations(sourceNodeId, maxDepth);
        return relations.stream().map(CheckedFunction.wrap(this::mapRelationData, null))
                .filter(Objects::nonNull)
                .toList();
    }

    private NodeRelationData mapRelationData(org.edu_sharing.service.relations.RelationData x) {
        return NodeRelationData.builder()
                // use getAnyExistingNode in case the original id it refers to has been deleted
                .fromNode(NodeDao.getAnyExistingNode(repoDao, Arrays.asList(NodeDao.ExistingMode.IfNotExists, NodeDao.ExistingMode.IfNoReadPermissions), x.getFromNode()).asNode())
                .toNode(NodeDao.getAnyExistingNode(repoDao, Arrays.asList(NodeDao.ExistingMode.IfNotExists, NodeDao.ExistingMode.IfNoReadPermissions), x.getToNode()).asNode())
                .creator(new User(authorityService.getUser(x.getCreatedBy())))
                .timestamp(x.getCreated())
                .type(x.getType())
                .reverseType(x.getReverseType())
                .isAiGenerated(x.isAiGenerated())
                .evaluation(x.getEvaluation())
                .metadata(x.getMetadata())
                .build();
    }

    private NodeRelationData mapSimpleRelationData(org.edu_sharing.service.relations.RelationData x) {
        return NodeRelationData.builder()
                .fromNode(NodeDao.getAsNodeSimple(x.getFromNode()))
                .toNode(NodeDao.getAsNodeSimple(x.getToNode()))
                .creator(new User(authorityService.getUser(x.getCreatedBy())))
                .timestamp(x.getCreated())
                .type(x.getType())
                .reverseType(x.getReverseType())
                .isAiGenerated(x.isAiGenerated())
                .evaluation(x.getEvaluation())
                .metadata(x.getMetadata())
                .build();
    }

    public NodeRelationData updateRelation(UpdateRelationRequest request) {
        return mapRelationData(this.relationService.updateRelation(request));
    }

    public List< org.edu_sharing.service.relations.RelationData> getTrackedRelation(Date after, Integer maxItems) {
        return relationService.getTrackedData(after, maxItems == null ? Limit.unlimited() : Limit.of(maxItems));
    }

    public List<org.edu_sharing.service.relations.RelationData> getDeletedTrackedData(Date after, Integer maxItems) {
        return relationService.getDeletedTrackedData(after, maxItems == null ? Limit.unlimited() : Limit.of(maxItems));
    }
}
