package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class UserQuota implements Serializable  {
    @JsonProperty private boolean enabled;
    @JsonProperty private long sizeCurrent;
    @JsonProperty private long sizeQuota;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSizeCurrent() {
        return sizeCurrent;
    }

    public void setSizeCurrent(long sizeCurrent) {
        this.sizeCurrent = sizeCurrent;
    }

    public long getSizeQuota() {
        return sizeQuota;
    }

    public void setSizeQuota(long sizeQuota) {
        this.sizeQuota = sizeQuota;
    }
}
