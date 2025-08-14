package org.edu_sharing.service.share;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareInfoData implements ShareInfo {
    Long id;
    String nodeId;
    String sharedBy;
    String sharedWith;
    ShareStatus shareStatus;
    ShareType shareType;
    Date timestamp;
}
