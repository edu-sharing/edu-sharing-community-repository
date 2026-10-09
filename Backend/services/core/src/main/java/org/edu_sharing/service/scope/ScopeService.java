package org.edu_sharing.service.scope;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Assignments of nodes to scopes. Implemented by the mongo plugin (MongoScopeService); without the plugin the
 * {@link ScopeServiceAdapter} is used and the feature stays inactive.
 * <p>
 * Scope definitions and the active scope are not part of this service, see {@link ScopeDefinitionService} and
 * {@link ScopeContextResolver}.
 * <p>
 * Implementations carry {@link ScopesFeature} on the type. Index data, change feed and lifecycle are exempt, so the
 * tracker and the cleanup keep working while the feature flag is off.
 */
public interface ScopeService {

    // --- read

    /**
     * @return the assignment of the node in the scope or null if there is none
     */
    ScopeAssignment getAssignment(String scopeId, String nodeId);

    /**
     * @return the history of the assignment, newest first
     */
    List<ScopeHistoryEntry> getHistory(String scopeId, String nodeId);

    /**
     * index fragment for the tracker (ADR-2). Child objects get the fragment of their parent.
     * Restricted to admins / the system user.
     */
    @ScopesFeature(requiresEnabled = false)
    ScopeIndexData getIndexData(String nodeId);

    /**
     * bulk variant of {@link #getIndexData(String)}, keyed by node id
     */
    @ScopesFeature(requiresEnabled = false)
    Map<String, ScopeIndexData> getIndexData(Collection<String> nodeIds);

    /**
     * change feed for the tracker (ADR-1), same cursor contract as the share info oplog:
     * entries ordered by (at, seq) ascending.
     * Restricted to admins / the system user.
     *
     * @param after    exclusive lower bound of {@link ScopeChange#getAt()}, null to start from the beginning
     * @param afterSeq without after: a standalone seq cursor (seq &gt; afterSeq). Together with after: tiebreaker
     *                 for entries sharing the exact same after timestamp
     * @param until    inclusive upper bound of {@link ScopeChange#getAt()}, may be null
     * @param maxItems maximum number of entries
     */
    @ScopesFeature(requiresEnabled = false)
    List<ScopeChange> getChanges(Date after, Long afterSeq, Date until, int maxItems);

    /**
     * permission check (source of truth: mongo, cached). True if the node (the original, references and published
     * copies have to be resolved by the caller) has a published entry in the scope.
     */
    boolean isPublishedInScope(String scopeId, String nodeId);

    // --- write

    /**
     * sets (merges) the given properties. A property with an empty list is removed from the entry. Creates the
     * entry if missing, derives published/excluded, writes one history entry per changed property and a change
     * feed entry.
     *
     * @throws IllegalArgumentException if a property is not in the field list, a value is not in the valuespace
     *                                  or a single value property gets more than one value
     */
    ScopeAssignment setProperties(String scopeId, String nodeId, Map<String, List<String>> properties, String reason);

    void deleteAssignment(String scopeId, String nodeId, String reason);

    /**
     * starts an asynchronous bulk job
     *
     * @return the id of the job
     */
    String startBulk(String scopeId, ScopeBulkRequest request);

    ScopeBulkStatus getBulkStatus(String scopeId, String jobId);

    // --- lifecycle

    /**
     * content was uploaded into the shared folder of the scope group (C4). Creates the entry with the uploadValues of
     * the field list (source UPLOAD), unless one exists. Called by the upload policy after the commit, as system
     *
     * @param uploader the user who uploaded the content (createdBy, history actor)
     */
    void onUpload(String scopeId, String nodeId, String uploader);

    /**
     * the original node was finally deleted (purged from the trashcan or deleted without archiving, ADR-3)
     */
    @ScopesFeature(requiresEnabled = false)
    void onIoPurged(String nodeId);

    /**
     * the scope group was deleted: remove all entries and the history of the scope immediately
     */
    @ScopesFeature(requiresEnabled = false)
    void onScopeGroupDeleted(String scopeId);
}
