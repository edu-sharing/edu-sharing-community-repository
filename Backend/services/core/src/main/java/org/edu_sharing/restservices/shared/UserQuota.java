package org.edu_sharing.restservices.shared;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserQuota implements Serializable  {
    private boolean enabled;
    private long sizeCurrent;
    private long sizeQuota;

    /**
     * count quotas (max. number of owned nodes), null if no limit is set for the type
     * the current values are only filled if the corresponding limit is set
     */
    private Long materialCountQuota;
    private Long materialCountCurrent;
    private Long collectionCountQuota;
    private Long collectionCountCurrent;
    private Long folderCountQuota;
    private Long folderCountCurrent;
}
