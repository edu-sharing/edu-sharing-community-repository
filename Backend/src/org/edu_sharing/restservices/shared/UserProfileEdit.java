package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The editable UserProfile from RestApi, extended for additional fields only relevant for editing
 */
public class UserProfileEdit extends UserProfile {
    private long sizeQuota;

    @JsonProperty
    public long getSizeQuota() {
        return sizeQuota;
    }

    public void setSizeQuota(long sizeQuota) {
        this.sizeQuota = sizeQuota;
    }
}
