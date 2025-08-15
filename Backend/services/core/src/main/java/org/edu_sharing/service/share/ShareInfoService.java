package org.edu_sharing.service.share;

import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShareInfoService {

    @Transactional
    void createShare(@NonNull String nodeId, @NonNull String sharedBy, @NonNull String sharedWith, @NonNull ShareType shareType);

    void rejectShare(@NonNull String nodeId);

    void unrejectShare(@NonNull String nodeId);

    void removeShares(List<Long> shareIds);

    void removeShare(String nodeId, String sharedBy, String sharedWith);

    List<ShareInfo> getShares(NodeRef nodeRef);

    List<ShareInfo> getShares(@NonNull List<Long> shareIds);
}
