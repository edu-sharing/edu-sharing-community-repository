package org.edu_sharing.service.share;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareInfoServiceImpl implements NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.BeforeDeleteNodePolicy, ShareInfoService, ShareInfoOpLogService {

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

        log.info("ShareInfoService initialized");
    }

    @Override
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
        if (childAssocRef.getTypeQName().equals(QName.createQName(CCConstants.CCM_TYPE_IO)) || childAssocRef.getTypeQName().equals(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
            retryingTransactionHelper.doInTransaction(() -> {
                List<Long> longs = shareInfoMapper.deleteByNodeId(childAssocRef.getChildRef().getId());
                List<ShareInfoOplogData> oplogs = longs.stream()
                        .map(id -> new ShareInfoOplogData(null, id, OpLogAction.DELETE, new Date()))
                        .toList();

                if (!oplogs.isEmpty()) {
                    shareInfoOpLogMapper.createAll(oplogs);
                }
                return null;
            });
        }
    }

    @Override
    public void beforeDeleteNode(NodeRef nodeRef) {
        String userName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USER_USERNAME);
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> longs = shareInfoMapper.deleteBySharedWithOrSharedByAndShareStatus(userName, userName, ShareStatus.SHARED);
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
        if(!ShareInfoContextHolder.getContext().isCreateSharesOnPermissionChanged()){
            return;
        }

        retryingTransactionHelper.doInTransaction(() -> {
            Set<String> sharesToRemove = event.permissions()
                    .stream()
                    .map(AccessPermission::getAuthority)
                    .collect(Collectors.toSet());

            List<Long> longs = shareInfoMapper.deleteAllByNodeIdAndSharedByAndShareStatusAndSharedWithIn(event.node_id(), event.creator(), ShareStatus.SHARED, sharesToRemove);
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
        if(!ShareInfoContextHolder.getContext().isCreateSharesOnPermissionChanged()){
            return;
        }

        retryingTransactionHelper.doInTransaction(() -> {
            Set<String> sharesToAdd = event.permissions()
                    .stream()
                    .map(AccessPermission::getAuthority)
                    .collect(Collectors.toSet());

            Set<String> knownShares = shareInfoMapper.getAllSharesByNodeId(event.node_id())
                    .stream()
                    .map(ShareInfoData::getSharedWith)
                    .collect(Collectors.toSet());

            sharesToAdd.removeAll(knownShares);

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
            shareInfoMapper.createAll(shareInfos);
            List<ShareInfoOplogData> oplogs = shareInfos.stream()
                    .map(x -> new ShareInfoOplogData(null, x.getId(), OpLogAction.CREATE, new Date()))
                    .toList();

            shareInfoOpLogMapper.createAll(oplogs);
            return null;
        });
    }

    @Override
    @Transactional
    public void createShare(@NonNull String nodeId, @NonNull String sharedBy, @NonNull String sharedWith, @NonNull ShareType shareType) {
        retryingTransactionHelper.doInTransaction(() -> {
            ShareInfoData shareInfoData = new ShareInfoData(null, nodeId, sharedBy, sharedWith, ShareStatus.SHARED, shareType, new Date());
            shareInfoMapper.create(shareInfoData);
            shareInfoOpLogMapper.create(new ShareInfoOplogData(null, shareInfoData.getId(), OpLogAction.CREATE, new Date()));
            return null;
        });
    }

    @Override
    public void rejectShare(@NonNull String nodeId) {
        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new IllegalStateException("System user cannot reject shares");
        }

        retryingTransactionHelper.doInTransaction(() -> {
            ShareInfoData shareInfoData = new ShareInfoData(null, nodeId, null, AuthenticationUtil.getRunAsUser(), ShareStatus.REJECTED, ShareType.AUTHORITY, new Date());
            shareInfoMapper.create(shareInfoData);
            shareInfoOpLogMapper.create(new ShareInfoOplogData(null, shareInfoData.getId(), OpLogAction.CREATE, new Date()));
            return null;
        });
    }

    @Override
    public void unrejectShare(@NonNull String nodeId) {
        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new IllegalStateException("System user cannot reject shares");
        }
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> ids = shareInfoMapper.deleteAllByNodeIdAndSharedByIsNullAndShareStatusAndSharedWithIn(nodeId, ShareStatus.REJECTED, List.of(AuthenticationUtil.getRunAsUser()));
            if(ids.isEmpty()){
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
        if (shareIds.isEmpty()) {
            return;
        }

        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> deletedIds = shareInfoMapper.deleteAll(shareIds);
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
        retryingTransactionHelper.doInTransaction(() -> {
            List<Long> longs = shareInfoMapper.deleteAllByNodeIdAndSharedByAndShareStatusAndSharedWithIn(nodeId, sharedBy, ShareStatus.SHARED, List.of(sharedWith));
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
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())) {
            throw new InsufficientPermissionException("You are not allowed to access all shares of this node");
        }
        return shareInfoMapper.getAllSharesByNodeId(nodeRef.getId()).stream().map(ShareInfo.class::cast).toList();
    }

    @Override
    public List<ShareInfo> getShares(@NonNull List<Long> shareIds) {
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())) {
            throw new InsufficientPermissionException("You are not allowed to access shares of this node");
        }

        if (shareIds.isEmpty()) {
            return List.of();
        }
        return shareInfoMapper.getAllSharesByIdIn(shareIds).stream().map(ShareInfo.class::cast).toList();
    }


    @Override
    public List<ShareInfoOplog> getOplogs(Long afterTxId, Date afterDate, int limit) {
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()
                && !authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())) {
            throw new InsufficientPermissionException("You are not allowed to access oplogs");
        }

        List<ShareInfoOplogData> oplogs;
        if (afterTxId != null) {
            oplogs = shareInfoOpLogMapper.getAllAfterId(afterTxId, limit);
        } else if (afterDate != null) {
            oplogs = shareInfoOpLogMapper.getAllAfterTimestamp(afterDate, limit);
        } else {
            oplogs = shareInfoOpLogMapper.getAll(limit);
        }

        return oplogs.stream().map(ShareInfoOplog.class::cast).toList();
    }
}
