package org.edu_sharing.service.relations;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodeRelationTraceService {

    private final RelationTraceSettings relationTraceSettings;
    private final RelationService relationService;

    public List<RelationData> traceRelations(String nodeId, Integer maxDepth) {
        if(maxDepth == null){
            maxDepth = relationTraceSettings.getMaxDepth();
        }

        if(maxDepth > relationTraceSettings.getMaxDepth()){
            throw new IllegalArgumentException("Max depth cannot exceed " + relationTraceSettings.getMaxDepth());
        }

        Set<RelationData> traceResult = new HashSet<>();

        Set<String> knownTraces = new HashSet<>();
        Set<String> nextTraceSet = new HashSet<>();
        nextTraceSet.add(nodeId);
        for (int i = 0; i < maxDepth; i++) {
            for (String traceNodeId : nextTraceSet) {
                List<RelationData> relations = relationService.getRelations(traceNodeId);
                traceResult.addAll(relations);

                nextTraceSet = relations
                        .stream()
                        .map(RelationData::getToNode)
                        .collect(Collectors.toSet());

                nextTraceSet.removeAll(knownTraces);
                knownTraces.addAll(nextTraceSet);
            }
        }

        return traceResult.stream().toList();
    }
}

