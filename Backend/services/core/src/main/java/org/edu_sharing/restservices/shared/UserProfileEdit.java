package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The editable UserProfile from RestApi, extended for additional fields only relevant for editing
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileEdit extends UserProfile {
    private long sizeQuota;
    /**
     * count quotas (max. number of owned nodes)
     * null: keep the current value, <= 0: remove the limit, > 0: set the limit
     */
    private Long materialCountQuota;
    private Long collectionCountQuota;
    private Long folderCountQuota;
}
