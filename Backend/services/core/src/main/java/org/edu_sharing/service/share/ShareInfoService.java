package org.edu_sharing.service.share;

import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

public interface ShareInfoService {
    void createShare(String nodeId, String sharedBy, String sharedWith, ShareType shareType);

    void removeShares(List<Long> shareIds);

    void removeShare(String nodeId, String sharedBy, String sharedWith);

    List<ShareInfo> getShares(NodeRef nodeRef);

    List<ShareInfo> getShares(@NonNull List<Long> shareIds);

    void rejectShare(List<Long> shareIds);

    void unrejectShare(List<Long> shareIds);
}
