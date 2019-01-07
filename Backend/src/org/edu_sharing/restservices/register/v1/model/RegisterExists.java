package org.edu_sharing.restservices.register.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterExists {
    @JsonProperty
    private boolean exists;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }
}
