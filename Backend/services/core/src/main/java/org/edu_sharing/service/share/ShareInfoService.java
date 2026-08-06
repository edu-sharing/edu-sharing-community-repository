package org.edu_sharing.service.share;

import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ShareInfoService {

    /**
     * @return {@code true} if a new ShareInfo row was created, {@code false} if an identical share
     * (same node, sharedBy, sharedWith, status and type) already existed and this call was a no-op.
     */
    boolean createShare(@NonNull String nodeId, @NonNull String sharedBy, @NonNull String sharedWith, @NonNull ShareType shareType);

    /**
     * @return {@code true} if a new ShareInfo row was created, {@code false} if an identical share
     * (same node, sharedBy, sharedWith, status and type) already existed and this call was a no-op.
     */
    boolean createShare(@NotNull String nodeId, @NotNull String sharedBy, @NotNull String sharedWith, @NotNull ShareType shareType, @NotNull Date date);

    void rejectShare(@NonNull String nodeId);

    void unrejectShare(@NonNull String nodeId);

    void removeShares(List<Long> shareIds);

    void removeShare(String nodeId, String sharedBy, String sharedWith);

    List<ShareInfo> getShares(NodeRef nodeRef);

    List<ShareInfo> getShares(@NonNull List<Long> shareIds);
}
