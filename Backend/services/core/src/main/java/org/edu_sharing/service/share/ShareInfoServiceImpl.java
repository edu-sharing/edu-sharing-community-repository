package org.edu_sharing.service.share;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeArchiveServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.permission.events.AddedPermissionsEvent;
import org.edu_sharing.service.permission.events.RemovedPermissionEvent;
import org.edu_sharing.service.share.ibatis.ShareInfoMapper;
import org.edu_sharing.service.share.ibatis.ShareInfoOpLogMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareInfoServiceImpl implements NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.BeforeDeleteNodePolicy, NodeArchiveServicePolicies.BeforePurgeNodePolicy, ShareInfoService, ShareInfoOpLogService {

    private final ShareInfoMapper shareInfoMapper;
    private final ShareInfoOpLogMapper shareInfoOpLogMapper;
    private final PolicyComponent policyComponent;
    private final NodeService nodeService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final AuthorityService authorityService;

    @PostConstruct
    void init() {
        policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeDeleteNodePolicy.QNAME,
                ContentModel.TYPE_PERSON,
                new JavaBehaviour(this, "beforeDeleteNode"));

        policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_IO),
                new JavaBehaviour(this, "onDeleteNode"));

        policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_MAP),
                new JavaBehaviour(this, "onDeleteNode"));

        policyComponent.bindClassBehaviour(NodeArchiveServicePolicies.BeforePurgeNodePolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_IO),
                new JavaBehaviour(this, "beforePurgeNode"));

        policyComponent.bindClassBehaviour(NodeArchiveServicePolicies.BeforePurgeNodePolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_MAP),
                new JavaBehaviour(this, "beforePurgeNode"));

        log.info("ShareInfoService initialized");
    }

    @Override
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
        log.debug("onDeleteNode: nodeId={}, isNodeArchived={}", childAssocRef.getChildRef().getId(), isNodeArchived);

        // node is only moved to the trashcan here - share infos are cleaned up once it is purged (see beforePurgeNode),
        // since the archive store is exempt from OnDeleteNodePolicy and this would never fire again for the purge itself
        if (isNodeArchived) {
            return;
        }

        // binding is already scoped to ccm:io/ccm:map (see init()), no further type check needed here
        deleteShareInfoForNode(childAssocRef.getChildRef().getId());
    }

    @Override
    public void beforePurgeNode(NodeRef nodeRef) {
        log.debug("beforePurgeNode: nodeId={}", nodeRef.getId());

        deleteShareInfoForNode(nodeRef.getId());

        // purging a folder does not fire beforePurgeNode for its children, so walk the primary
        // hierarchy ourselves - this must happen recursively, since maps can be nested arbitrarily
        // deep. currently only io/map children are handled, since those are the only types that
        // can carry share infos today - if other types become share-able in the future, they need
        // to be added to isIoOrMap() as well
        if (QName.createQName(CCConstants.CCM_TYPE_MAP).equals(nodeService.getType(nodeRef))) {
            nodeService.getChildAssocs(nodeRef).stream()
                    .filter(ChildAssociationRef::isPrimary)
                    .map(ChildAssociationRef::getChildRef)
                    .filter(this::isIoOrMap)
                    .forEach(this::beforePurgeNode);
        }
    }

    private boolean isIoOrMap(NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        return QName.createQName(CCConstants.CCM_TYPE_IO).equals(type) || QName.createQName(CCConstants.CCM_TYPE_MAP).equals(type);
    }

    private void deleteShareInfoForNode(String nodeId) {
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> longs = shareInfoMapper.deleteByNodeId(nodeId);
            log.debug("deleteShareInfoForNode: nodeId={}, deletedShareIds={}", nodeId, longs);
            List<ShareInfoOplogData> oplogs = longs.stream()
                    .map(id -> new ShareInfoOplogData(null, id, OpLogAction.DELETE, new Date()))
                    .toList();

            if (!oplogs.isEmpty()) {
                shareInfoOpLogMapper.createAll(oplogs);
            }
            return null;
        });
    }

    @Override
    public void beforeDeleteNode(NodeRef nodeRef) {
        String userName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USER_USERNAME);
        log.debug("beforeDeleteNode: nodeId={}, userName={}", nodeRef.getId(), userName);
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> longs = shareInfoMapper.deleteBySharedWithOrSharedByAndShareStatus(userName, userName, ShareStatus.SHARED);
            log.debug("beforeDeleteNode: userName={}, deletedShareIds={}", userName, longs);
            List<ShareInfoOplogData> oplogs = longs.stream()
                    .map(id -> new ShareInfoOplogData(null, id, OpLogAction.DELETE, new Date()))
                    .toList();

            if (!oplogs.isEmpty()) {
                shareInfoOpLogMapper.createAll(oplogs);
            }
            return null;
        });
    }


    @EventListener
    public void onRemovedPermissionEvent(RemovedPermissionEvent event) {
        log.debug("onRemovedPermissionEvent: nodeId={}, creator={}, permissions={}", event.node_id(), event.creator(), event.permissions());

        if (!ShareInfoContextHolder.getContext().isCreateSharesOnPermissionChanged()) {
            log.debug("onRemovedPermissionEvent: skipped, createSharesOnPermissionChanged is false");
            return;
        }

        retryingTransactionHelper.doInTransaction(() -> {
            Set<String> sharesToRemove = event.permissions()
                    .stream()
                    .map(AccessPermission::getAuthority)
                    .collect(Collectors.toSet());

            List<Long> longs = shareInfoMapper.deleteAllByNodeIdAndSharedByAndShareStatusAndSharedWithIn(event.node_id(), event.creator(), ShareStatus.SHARED, sharesToRemove);
            log.debug("onRemovedPermissionEvent: nodeId={}, sharesToRemove={}, deletedShareIds={}", event.node_id(), sharesToRemove, longs);
            List<ShareInfoOplogData> oplogs = longs.stream()
                    .map(id -> new ShareInfoOplogData(null, id, OpLogAction.DELETE, new Date()))
                    .toList();

            if (!oplogs.isEmpty()) {
                shareInfoOpLogMapper.createAll(oplogs);
            }
            return null;
        });
    }

    @EventListener
    public void onAddedPermissionEvent(AddedPermissionsEvent event) {
        log.debug("onAddedPermissionEvent: nodeId={}, creator={}, permissions={}", event.node_id(), event.creator(), event.permissions());

        if (!ShareInfoContextHolder.getContext().isCreateSharesOnPermissionChanged()) {
            log.debug("onAddedPermissionEvent: skipped, createSharesOnPermissionChanged is false");
            return;
        }

        Exception error = retryingTransactionHelper.doInTransaction(() -> {
            Set<String> sharesToAdd = event.permissions()
                    .stream()
                    .map(AccessPermission::getAuthority)
                    .collect(Collectors.toSet());

            Set<String> knownShares = shareInfoMapper.getAllSharesByNodeId(event.node_id())
                    .stream()
                    .map(ShareInfoData::getSharedWith)
                    .collect(Collectors.toSet());

            log.debug("onAddedPermissionEvent: nodeId={}, sharesFromEvent={}, knownShares={}", event.node_id(), sharesToAdd, knownShares);
            sharesToAdd.removeAll(knownShares);
            log.debug("onAddedPermissionEvent: nodeId={}, newSharesToAdd={}", event.node_id(), sharesToAdd);

            if (sharesToAdd.isEmpty()) {
                return null;
            }

            List<ShareInfoData> shareInfos = sharesToAdd.stream()
                    .map(shareWith -> new ShareInfoData(
                            null,
                            event.node_id(),
                            event.creator(),
                            shareWith,
                            ShareStatus.SHARED,
                            ShareType.AUTHORITY,
                            new Date()
                    )).toList();
            try {
                shareInfoMapper.createAll(shareInfos);

                List<ShareInfoOplogData> oplogs = shareInfos.stream()
                        .map(x -> new ShareInfoOplogData(null, x.getId(), OpLogAction.CREATE, new Date()))
                        .toList();

                shareInfoOpLogMapper.createAll(oplogs);
            } catch (DuplicateKeyException e) {
                log.warn("Some shares already exists: {}", shareInfos);
                return e;
            }
            return null;
        });

        if (error != null) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public void createShare(@NonNull String nodeId, @NonNull String sharedBy, @NonNull String sharedWith, @NonNull ShareType shareType) {
        createShare(nodeId, sharedBy, sharedWith, shareType, new Date());
    }

    @Override
    public void createShare(@NotNull String nodeId, @NotNull String sharedBy, @NotNull String sharedWith, @NotNull ShareType shareType, @NotNull Date date) {
        log.debug("createShare: nodeId={}, sharedBy={}, sharedWith={}, shareType={}, date={}", nodeId, sharedBy, sharedWith, shareType, date);
        Exception error = retryingTransactionHelper.doInTransaction(() -> {
            ShareInfoData shareInfoData = new ShareInfoData(null, nodeId, sharedBy, sharedWith, ShareStatus.SHARED, shareType, date);
            try {
                shareInfoMapper.create(shareInfoData);
                shareInfoOpLogMapper.create(new ShareInfoOplogData(null, shareInfoData.getId(), OpLogAction.CREATE, date));
                return null;
            } catch (DuplicateKeyException e) {
                log.warn("Share already exists: {}", shareInfoData);
                return e;
            }
        });

        if (error != null) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public void rejectShare(@NonNull String nodeId) {
        String userName = AuthenticationUtil.getRunAsUser();
        log.debug("rejectShare: nodeId={}, userName={}", nodeId, userName);

        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new IllegalStateException("System user cannot reject shares");
        }

        retryingTransactionHelper.doInTransaction(() -> {
            try {
                ShareInfoData shareInfoData = new ShareInfoData(null, nodeId, null, userName, ShareStatus.REJECTED, ShareType.AUTHORITY, new Date());
                shareInfoMapper.create(shareInfoData);
                shareInfoOpLogMapper.create(new ShareInfoOplogData(null, shareInfoData.getId(), OpLogAction.CREATE, new Date()));
            } catch (DuplicateKeyException e) {
                log.debug("rejectShare: nodeId={}, userName={} already rejected", nodeId, userName);
                return null;
            }
            return null;
        });
    }

    @Override
    public void unrejectShare(@NonNull String nodeId) {
        log.debug("unrejectShare: nodeId={}, userName={}", nodeId, AuthenticationUtil.getRunAsUser());

        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new IllegalStateException("System user cannot reject shares");
        }
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> ids = shareInfoMapper.deleteAllByNodeIdAndSharedByIsNullAndShareStatusAndSharedWithIn(nodeId, ShareStatus.REJECTED, List.of(AuthenticationUtil.getRunAsUser()));
            log.debug("unrejectShare: nodeId={}, deletedShareIds={}", nodeId, ids);
            if (ids.isEmpty()) {
                return null;
            }

            List<ShareInfoOplogData> oplogs = ids.stream()
                    .map(x -> new ShareInfoOplogData(null, x, OpLogAction.DELETE, new Date()))
                    .toList();
            shareInfoOpLogMapper.createAll(oplogs);
            return null;
        });
    }

    @Override
    public void removeShares(List<Long> shareIds) {
        log.debug("removeShares: shareIds={}", shareIds);

        if (shareIds.isEmpty()) {
            return;
        }

        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> deletedIds = shareInfoMapper.deleteAll(shareIds);
            log.debug("removeShares: deletedShareIds={}", deletedIds);
            List<ShareInfoOplogData> oplogs = deletedIds.stream()
                    .map(x -> new ShareInfoOplogData(null, x, OpLogAction.DELETE, new Date()))
                    .toList();
            if (!oplogs.isEmpty()) {
                shareInfoOpLogMapper.createAll(oplogs);
            }
            return null;
        });
    }


    @Override
    @Transactional
    public void removeShare(@NonNull String nodeId, @NonNull String sharedBy, @NonNull String sharedWith) {
        log.debug("removeShare: nodeId={}, sharedBy={}, sharedWith={}", nodeId, sharedBy, sharedWith);
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> longs = shareInfoMapper.deleteAllByNodeIdAndSharedByAndShareStatusAndSharedWithIn(nodeId, sharedBy, ShareStatus.SHARED, List.of(sharedWith));
            log.debug("removeShare: nodeId={}, deletedShareIds={}", nodeId, longs);
            List<ShareInfoOplogData> oplogs = longs.stream()
                    .map(id -> new ShareInfoOplogData(null, id, OpLogAction.DELETE, new Date()))
                    .toList();
            if (!oplogs.isEmpty()) {
                shareInfoOpLogMapper.createAll(oplogs);
            }
            return null;
        });
    }

    @Override
    public List<ShareInfo> getShares(@NonNull NodeRef nodeRef) {
        String userName = AuthenticationUtil.getRunAsUser();
        log.debug("getShares: nodeId={}, userName={}", nodeRef.getId(), userName);
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(userName)) {
            log.debug("getShares: nodeId={}, userName={} is not admin, denying access", nodeRef.getId(), userName);
            throw new InsufficientPermissionException("You are not allowed to access all shares of this node");
        }
        List<ShareInfo> shares = shareInfoMapper.getAllSharesByNodeId(nodeRef.getId()).stream().map(ShareInfo.class::cast).toList();
        log.debug("getShares: nodeId={}, returning {} shares", nodeRef.getId(), shares.size());
        return shares;
    }

    @Override
    public List<ShareInfo> getShares(@NonNull List<Long> shareIds) {
        String userName = AuthenticationUtil.getRunAsUser();
        log.debug("getShares: shareIds={}, userName={}", shareIds, userName);
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(userName)) {
            log.debug("getShares: shareIds={}, userName={} is not admin, denying access", shareIds, userName);
            throw new InsufficientPermissionException("You are not allowed to access shares of this node");
        }

        if (shareIds.isEmpty()) {
            return List.of();
        }
        List<ShareInfo> shares = shareInfoMapper.getAllSharesByIdIn(shareIds).stream().map(ShareInfo.class::cast).toList();
        log.debug("getShares: shareIds={}, returning {} shares", shareIds, shares.size());
        return shares;
    }


    /**
     * Retrieves a list of ShareInfoOplog entries based on the specified parameters.
     *
     * @param afterTxId the transaction ID after which the oplogs should be fetched;
     *                  if null, this parameter will be ignored.
     * @param afterDate the start date after which the oplogs should be fetched;
     *                  if null, this parameter will be ignored. (exclusive)
     * @param untilDate the end date until which the oplogs should be fetched;
     *                  if null, this parameter will be ignored. (inklusiv)
     * @param limit     the maximum number of oplogs to retrieve.
     * @return a list of ShareInfoOplog objects matching the specified criteria.
     * @throws InsufficientPermissionException if the current user does not have sufficient permissions
     *                                         to access the oplogs.
     */
    @Override
    public List<ShareInfoOplog> getOplogs(Long afterTxId, Date afterDate, Date untilDate, int limit) {
        log.debug("getOplogs: afterTxId={}, afterDate={}, untilDate={}, limit={}", afterTxId, afterDate, untilDate, limit);

        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())) {
            throw new InsufficientPermissionException("You are not allowed to access oplogs");
        }

        List<ShareInfoOplogData> oplogs;
        if (afterTxId != null) {
            oplogs = shareInfoOpLogMapper.getAllAfterId(afterTxId, limit);
        } else if (afterDate != null) {
            oplogs = untilDate != null
                    ? shareInfoOpLogMapper.getAllBetweenTimestamp(afterDate, untilDate, limit)
                    : shareInfoOpLogMapper.getAllAfterTimestamp(afterDate, limit);
        } else {
            oplogs = shareInfoOpLogMapper.getAll(limit);
        }

        log.debug("getOplogs: returning {} entries", oplogs.size());
        return oplogs.stream().map(ShareInfoOplog.class::cast).toList();
    }
}
