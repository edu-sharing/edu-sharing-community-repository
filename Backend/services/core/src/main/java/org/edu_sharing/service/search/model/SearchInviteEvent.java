package org.edu_sharing.service.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.edu_sharing.generated.repository.backend.services.rest.client.model.ShareInfo;
import org.edu_sharing.service.model.NodeRef;

import java.util.Date;

@AllArgsConstructor
@Data
public class SearchInviteEvent {
    NodeRef nodeRef;
    String sharedBy;
    String sharedWith;
    Date timestamp;
    ShareInfo.ShareTypeEnum shareType;
    ShareInfo.ShareStatusEnum shareStatus;
}
