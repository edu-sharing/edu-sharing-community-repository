package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.ShareInfoServiceImpl;
import org.edu_sharing.service.share.ShareType;
import org.springframework.dao.DuplicateKeyException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_ShareInfos {

    /**
     * version nodes carry the ph_* properties (frozen 1:1 by Version2ServiceImpl.freezeAspects) but
     * their child associations are frozen as empty reference nodes (no token/email/creator), so link
     * shares can't be reconstructed there, and the version node's own id doesn't refer to any live
     * node. For this store, only the leftover properties are cleaned up - no ShareInfo is created.
     */
    private static final StoreRef VERSION_STORE = new StoreRef("workspace", "version2Store");

    /**
     * cap on how many unmigrated node refs are spelled out in the final error log line - the exact
     * count is always logged alongside it, this only bounds the log line's size for a mass failure.
     */
    private static final int MAX_LOGGED_FAILED_NODES = 100;

    private final NodeService nodeService;
    private final ShareInfoServiceImpl shareInfoService;
    private final GlobalShareService globalShareService;

    /**
     * Aggregated counters for a single run, so the outcome can be judged from the summary line alone
     * without turning on debug logging for a potentially multi-million-node run.
     * <p>
     * {@code failed} is a set rather than a counter: {@link NodeRunner}'s LocalRetrying transaction
     * mode can re-run a node's task after a failed attempt, so a node is only "finally" failed if it
     * never later completes successfully - see {@link #execute(boolean)}.
     */
    private static class Stats {
        final AtomicInteger processed = new AtomicInteger();
        final AtomicInteger versionStoreCleanupOnly = new AtomicInteger();
        final AtomicInteger vanished = new AtomicInteger();
        final AtomicInteger nothingToMigrate = new AtomicInteger();
        final AtomicInteger linkShares = new AtomicInteger();
        final AtomicInteger authorityShares = new AtomicInteger();
        final AtomicInteger duplicatesSkipped = new AtomicInteger();
        final AtomicInteger sharesSkippedDueToMissingData = new AtomicInteger();
        final AtomicInteger dateFallback = new AtomicInteger();
        final AtomicInteger incompleteNodes = new AtomicInteger();
        final Set<NodeRef> failed = ConcurrentHashMap.newKeySet();
    }

    @UpdateRoutine(
            id = "Release_11_0_ShareInfos",
            description = "Migrate ph_users and ph_invite to shareInfo",
            order = 11000,
            auto = true,
            isNonTransactional = true,
            async = true,
            blocking = false)
    public void execute(boolean test) {
        NodeRunner runner = new NodeRunner();
        runner.setRunAsSystem(true);
        runner.setTypes(List.of(CCConstants.CCM_TYPE_IO, CCConstants.CCM_TYPE_MAP));
        runner.setThreaded(true);
        runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
        runner.setKeepModifiedDate(true);
        // only nodes that ever had permissions set or a link share created carry one of these aspects -
        // collecting by aspect avoids traversing the whole (workspace/archive/version) repository tree
        runner.setAspects(List.of(CCConstants.CCM_ASPECT_PERMISSION_HISTORY, CCConstants.CCM_ASPECT_SHARES));
        runner.setAspectStores(List.of(
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                StoreRef.STORE_REF_ARCHIVE_SPACESSTORE,
                VERSION_STORE));

        Stats stats = new Stats();
        long startTime = System.currentTimeMillis();

        runner.setTask(nodeRef -> {
            try {
                migrateNode(nodeRef, test, stats);
                // a node that completes successfully after a previous failed attempt (LocalRetrying
                // re-runs it) is no longer part of the final failure list
                stats.failed.remove(nodeRef);

                int done = stats.processed.incrementAndGet();
                if (done % 1000 == 0) {
                    long elapsedSeconds = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
                    log.info("ShareInfos update progress: {} node(s) processed, {} failed so far, {}s elapsed, {} node(s)/s",
                            done, stats.failed.size(), elapsedSeconds, done / elapsedSeconds);
                }
            } catch (Exception e) {
                // NodeRunner runs tasks in a thread pool and never reads back the per-node Futures, so
                // without this the node would fail silently - the LocalRetrying transaction still rolls
                // back and leaves the node (incl. its ph_* properties) untouched for this attempt.
                // This is logged as a WARN, not an ERROR: LocalRetrying may re-run this exact node and
                // succeed, so a single failed attempt is not yet final - see the summary logged after
                // runner.run() below for the authoritative list of nodes that never succeeded.
                stats.failed.add(nodeRef);
                log.warn("ShareInfos migration attempt failed for {} (may be retried): {}", nodeRef, e.getMessage(), e);
                throw e;
            }
        });

        log.info("Starting ShareInfos update (test={}, stores={}, aspects={}, types={}, threads={})",
                test, runner.getAspectStores(), runner.getAspects(), runner.getTypes(), OAIPMHLOMImporter.getThreadCount());
        int collected = runner.run();
        long durationSeconds = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);

        log.info("ShareInfos update finished (test={}): collected={}, migrated={}, versionStoreCleanupOnly={}, " +
                        "vanished={}, nothingToMigrate={}, linkShares={}, authorityShares={}, duplicatesSkipped={}, " +
                        "sharesSkippedDueToMissingData={}, dateFallbackToCmModified={}, incompleteNodes={}, failedNodes={}, duration={}s",
                test, collected, stats.processed.get(), stats.versionStoreCleanupOnly.get(), stats.vanished.get(),
                stats.nothingToMigrate.get(), stats.linkShares.get(), stats.authorityShares.get(),
                stats.duplicatesSkipped.get(), stats.sharesSkippedDueToMissingData.get(), stats.dateFallback.get(),
                stats.incompleteNodes.get(), stats.failed.size(), durationSeconds);

        if (!stats.failed.isEmpty()) {
            List<NodeRef> firstFailed = stats.failed.stream().limit(MAX_LOGGED_FAILED_NODES).toList();
            // the sysupdate protocol entry (see UpdaterServiceImpl.executeUpdate) is written regardless
            // of per-node failures and there is no API to reset it, so these nodes stay unmigrated until
            // someone notices this line and re-runs the migration for them explicitly
            log.error("ShareInfos migration left {} node(s) permanently unmigrated after all retries " +
                            "(showing the first {}): {}",
                    stats.failed.size(), firstFailed.size(), firstFailed);
        }
    }

    /**
     * Migrates a single node's ph_users/ph_invited/ph_modified properties to ShareInfo rows (or, for
     * version-store nodes, only cleans them up - see {@link #VERSION_STORE}) and updates {@code stats}.
     */
    @SuppressWarnings("unchecked")
    private void migrateNode(NodeRef nodeRef, boolean test, Stats stats) {
        log.debug("Processing {}", nodeRef);

        // the routine runs on a live system, so a node collected earlier may have been deleted or purged
        // from the trashcan by the time its task actually runs - this is a benign race, not a failure
        if (!nodeService.exists(nodeRef)) {
            log.debug("Node {} no longer exists, skipping (likely deleted or purged concurrently)", nodeRef);
            stats.vanished.incrementAndGet();
            return;
        }

        boolean isVersionStore = VERSION_STORE.equals(nodeRef.getStoreRef());

        String creator = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);

        List<String> rawUsers = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
        List<String> rawInvited = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
        Date rawDate = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));

        if (rawUsers == null) rawUsers = new ArrayList<>();
        if (rawInvited == null) rawInvited = new ArrayList<>();
        if (rawDate == null) {
            rawDate = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIED));
            if (rawDate != null) {
                stats.dateFallback.incrementAndGet();
                log.debug("ccm:ph_modified missing for {}, falling back to cm:modified={}", nodeRef, rawDate);
            }
        }

        // version nodes: only clean up the leftover properties below, shares are not reconstructable there
        if (!isVersionStore) {
            if (rawDate == null) {
                // neither ccm:ph_modified nor the cm:modified fallback are set - without a date we can't
                // populate the NOT NULL timestamp column, so fail loudly and leave the node for a re-run
                // instead of hitting an opaque constraint violation inside ShareInfoServiceImpl.createShare
                throw new IllegalStateException("Neither ccm:ph_modified nor cm:modified is set for "
                        + nodeRef + ", can't determine a share timestamp");
            }

            Set<String> users = new HashSet<>(rawUsers);
            Set<String> invited = new HashSet<>(rawInvited);
            log.debug("Found {} users and {} invited for {}", users.size(), invited.size(), nodeRef);

            if (users.isEmpty() && invited.isEmpty()) {
                stats.nothingToMigrate.incrementAndGet();
            }

            // we don't really know who shared all materials. So by default, we use the creator of the node for the sharedBy user.
            // Except he doesn't share the material at all (not in the list of users) otherwise we take the first user in the list.
            String sharedBy = creator;
            if (!rawUsers.isEmpty() && !users.contains(creator)) {
                sharedBy = rawUsers.get(0);
            }
            if (sharedBy == null) {
                // no per-node fallback left (no creator, no ph_users) - authority shares can't be
                // created at all without a sharedBy, and link shares fall back to this same value,
                // so both paths below will skip affected shares individually and count them
                log.warn("Could not determine a sharedBy user for {} (creator={}, ph_users={}), affected shares will be skipped",
                        nodeRef, creator, rawUsers);
            }

            Share[] shares = globalShareService.getShares(nodeRef);
            log.debug("Found {} shares for {}", shares.length, nodeRef);
            for (Share share : shares) {
                String shareNodeId = share.getNodeId();
                if (StringUtils.isBlank(shareNodeId)) {
                    log.warn("Skipping a link share for {} with a blank share node id", nodeRef);
                    stats.sharesSkippedDueToMissingData.incrementAndGet();
                    continue;
                }
                NodeRef nodeRefShare = new NodeRef(nodeRef.getStoreRef(), shareNodeId);
                String shareCreator = (String) nodeService.getProperty(nodeRefShare, ContentModel.PROP_CREATOR);
                if (shareCreator == null) {
                    shareCreator = sharedBy;
                    if (shareCreator == null) {
                        // neither the share node's own creator nor the node-level sharedBy heuristic
                        // could resolve a user - nothing sensible to store, so skip just this share
                        log.warn("Skipping link share {} for {}: neither the share's creator nor a sharedBy fallback could be determined", nodeRefShare, nodeRef);
                        stats.sharesSkippedDueToMissingData.incrementAndGet();
                        continue;
                    }
                    log.warn("Link share {} for {} has no creator, falling back to sharedBy={}", nodeRefShare, nodeRef, shareCreator);
                }

                try {
                    if (!test) {
                        shareInfoService.createShare(nodeRef.getId(), shareCreator, shareNodeId, ShareType.LINK, rawDate);
                    }
                    log.debug("Created link share for {}: by: {} - with: {}", nodeRef, shareCreator, nodeRefShare);
                } catch (RuntimeException e) {
                    if (isDuplicateShare(e)) {
                        log.debug("Link share for {} by {} with {} already exists, skipping", nodeRef, shareCreator, nodeRefShare);
                        stats.duplicatesSkipped.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                stats.linkShares.incrementAndGet();
                users.remove(shareCreator);
            }

            users.remove(sharedBy);
            for (String authority : invited) {
                if (StringUtils.isBlank(authority)) {
                    log.warn("Skipping an authority share for {} with a blank sharedWith authority", nodeRef);
                    stats.sharesSkippedDueToMissingData.incrementAndGet();
                    continue;
                }
                if (sharedBy == null) {
                    // unlike link shares, there's no per-share fallback here (no share node to read a
                    // creator from) - without a sharedBy the row would record an invite from nobody
                    log.warn("Skipping authority share for {} with {}: no sharedBy could be determined", nodeRef, authority);
                    stats.sharesSkippedDueToMissingData.incrementAndGet();
                    continue;
                }
                try {
                    if (!test) {
                        shareInfoService.createShare(nodeRef.getId(), sharedBy, authority, ShareType.AUTHORITY, rawDate);
                    }
                    log.debug("Created authority share for {}: by: {} - with: {}", nodeRef, sharedBy, authority);
                } catch (RuntimeException e) {
                    if (isDuplicateShare(e)) {
                        log.debug("Authority share for {} by {} with {} already exists, skipping", nodeRef, sharedBy, authority);
                        stats.duplicatesSkipped.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                stats.authorityShares.incrementAndGet();
            }

            if (!users.isEmpty()) {
                stats.incompleteNodes.incrementAndGet();
                log.warn("ShareInfos for {} are not complete. Missing users: {} (creator={}, sharedBy={}, ph_users={}, ph_invited={}, linkShares={})",
                        nodeRef, String.join(",", users), creator, sharedBy, rawUsers, rawInvited, shares.length);
            }
        } else {
            stats.versionStoreCleanupOnly.incrementAndGet();
        }
        if (!test) {
            removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), isVersionStore);
            removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), isVersionStore);
            removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), isVersionStore);
            removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), isVersionStore);
        }
        log.debug("ShareInfos for {} updated", nodeRef);
    }

    /**
     * ShareInfoServiceImpl.createShare wraps the DuplicateKeyException from its retrying transaction
     * into a plain RuntimeException (see ShareInfoServiceImpl.createShare), so callers can't catch
     * DuplicateKeyException directly and must inspect the cause instead. Duplicates are expected here
     * since this routine can run while the system is live - e.g. a permission change may have already
     * created the same ShareInfo via onAddedPermissionEvent before this node was migrated.
     */
    private boolean isDuplicateShare(RuntimeException e) {
        return e.getCause() instanceof DuplicateKeyException;
    }

    /**
     * removes a property both under its regular QName and, for version nodes, under the
     * {@code ver2:metadata-}-prefixed QName that {@link org.alfresco.repo.version.Version2ServiceImpl}
     * uses for frozen aspect properties - same pattern as {@code BulkEditNodesJob.removeProperty}.
     */
    private void removeProperty(NodeRef nodeRef, QName qName, boolean isVersionStore) {
        nodeService.removeProperty(nodeRef, qName);
        if (isVersionStore) {
            nodeService.removeProperty(nodeRef,
                    QName.createQName(Version2Model.NAMESPACE_URI, Version2Model.PROP_METADATA_PREFIX + qName.toString()));
        }
    }
}
