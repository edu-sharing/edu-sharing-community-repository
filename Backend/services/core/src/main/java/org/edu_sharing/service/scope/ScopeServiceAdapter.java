package org.edu_sharing.service.scope;

import org.edu_sharing.service.permission.annotation.Permission;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Fallback {@link ScopeService} for contexts without scope storage (no mongo plugin, remote repositories).
 * Reads return empty results so search, permissions and node data behave as without scopes, writes are rejected.
 */
@Lazy
@Service
@ScopesFeature
public class ScopeServiceAdapter implements ScopeService {

    private static final String NOT_AVAILABLE = "scopes are not available in this context (requires the mongo plugin)";

    @Override
    public ScopeAssignment getAssignment(String scopeId, String nodeId) {
        return null;
    }

    @Override
    public List<ScopeHistoryEntry> getHistory(String scopeId, String nodeId) {
        return List.of();
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public ScopeIndexData getIndexData(String nodeId) {
        return ScopeIndexData.builder()
                .nodeId(nodeId)
                .scopeIds(List.of())
                .scopeIdsPublished(List.of())
                .scopeIdsExcluded(List.of())
                .scopeOverrides(List.of())
                .scopes(List.of())
                .build();
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public Map<String, ScopeIndexData> getIndexData(Collection<String> nodeIds) {
        Map<String, ScopeIndexData> result = new HashMap<>();
        nodeIds.forEach(nodeId -> result.put(nodeId, getIndexData(nodeId)));
        return result;
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public List<ScopeChange> getChanges(Date after, Long afterSeq, Date until, int maxItems) {
        return List.of();
    }

    @Override
    public boolean isPublishedInScope(String scopeId, String nodeId) {
        return false;
    }

    @Override
    public ScopeAssignment setProperties(String scopeId, String nodeId, Map<String, List<String>> properties, String reason) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }

    @Override
    public void deleteAssignment(String scopeId, String nodeId, String reason) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }

    @Override
    public String startBulk(String scopeId, ScopeBulkRequest request) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }

    @Override
    public ScopeBulkStatus getBulkStatus(String scopeId, String jobId) {
        return null;
    }

    @Override
    public String startFolderImport(String scopeId) {
        return null;
    }

    @Override
    public void onUpload(String scopeId, String nodeId, String uploader) {
    }

    @Override
    public void onIoPurged(String nodeId) {
    }

    @Override
    public void onScopeGroupDeleted(String scopeId) {
    }

    @Override
    public void onDefinitionChanged(String scopeId) {
    }
}
