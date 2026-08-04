package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.ShareInfoServiceImpl;
import org.edu_sharing.service.share.ShareType;
import org.springframework.dao.DuplicateKeyException;

import java.util.*;
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

    private final NodeService nodeService;
    private final ShareInfoServiceImpl shareInfoService;
    private final GlobalShareService globalShareService;

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

        AtomicInteger progress = new AtomicInteger();

        runner.setTask(nodeRef -> {
            log.debug("Processing {}", nodeRef);
            boolean isVersionStore = VERSION_STORE.equals(nodeRef.getStoreRef());

            String creator = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);

            List<String> rawUsers = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
            List<String> rawInvited = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
            Date rawDate = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));

            if (rawUsers == null) rawUsers = new ArrayList<>();
            if (rawInvited == null) rawInvited = new ArrayList<>();
            if (rawDate == null)
                rawDate = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIED));

            // version nodes: only clean up the leftover properties below, shares are not reconstructable there
            if (!isVersionStore) {
                Set<String> users = new HashSet<>(rawUsers);
                Set<String> invited = new HashSet<>(rawInvited);
                log.debug("Found {} users and {} invited for {}", users.size(), invited.size(), nodeRef.getId());

                // we don't really know who shared all materials. So by default, we use the creator of the node for the sharedBy user.
                // Except he doesn't share the material at all (not in the list of users) otherwise we take the first user in the list.
                String sharedBy = creator;
                if (!rawUsers.isEmpty() && !users.contains(creator)) {
                    sharedBy = rawUsers.get(0);
                }

                Share[] shares = globalShareService.getShares(nodeRef);
                log.debug("Found {} shares for {}", shares.length, nodeRef.getId());
                for (Share share : shares) {
                    String shareNodeId = share.getNodeId();
                    NodeRef nodeRefShare = new NodeRef(nodeRef.getStoreRef(), shareNodeId);
                    String shareCreator = (String) nodeService.getProperty(nodeRefShare, ContentModel.PROP_CREATOR);

                    try {
                        if (!test) {
                            shareInfoService.createShare(nodeRef.getId(), shareCreator, shareNodeId, ShareType.LINK, rawDate);
                        }
                        log.debug("Created link share for {}: by: {} - with: {}", nodeRef.getId(), shareCreator, share.getNodeId());
                    } catch (DuplicateKeyException ignored) {
                    }
                    users.remove(shareCreator);
                }

                users.remove(sharedBy);
                for (String authority : invited) {
                    try {
                        if (!test) {
                            shareInfoService.createShare(nodeRef.getId(), sharedBy, authority, ShareType.AUTHORITY, rawDate);
                        }
                        log.debug("Created authority share for {}: by: {} - with: {}", nodeRef.getId(), creator, authority);
                    } catch (DuplicateKeyException ignored) {
                    }
                }

                if (!users.isEmpty()) {
                    log.warn("ShareInfos for {} are not complete. Missing users: {}", nodeRef.getId(), String.join(",", users));
                }
            }
            if (!test) {
                removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), isVersionStore);
                removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), isVersionStore);
                removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), isVersionStore);
                removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), isVersionStore);
            }
            log.debug("ShareInfos for {} updated", nodeRef.getId());

            int done = progress.incrementAndGet();
            if (done % 1000 == 0) {
                log.info("ShareInfos update progress: {} node(s) processed", done);
            }
        });

        log.info("Starting ShareInfos update");
        int processed = runner.run();
        log.info("ShareInfos for all nodes updated. Processed: {}", processed);
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
