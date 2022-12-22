package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UserRender extends UserSimple {
    private String primaryAffiliation;
    private List<String> remoteRoles;

    private boolean isGuest;

    @JsonProperty
    public String getPrimaryAffiliation() {
        return primaryAffiliation;
    }

    public void setPrimaryAffiliation(String primaryAffiliation) {
        this.primaryAffiliation = primaryAffiliation;
    }

    @JsonProperty
    public List<String> getRemoteRoles() {
        return remoteRoles;
    }

    public void setRemoteRoles(List<String> remoteRoles) {
        this.remoteRoles = remoteRoles;
    }

    @JsonProperty
    public boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(boolean loginStatusCode) {
        this.isGuest = loginStatusCode;
    }
}
