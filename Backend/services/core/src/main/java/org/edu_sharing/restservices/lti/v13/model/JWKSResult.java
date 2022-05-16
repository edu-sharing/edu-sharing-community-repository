package org.edu_sharing.restservices.lti.v13.model;


import java.util.List;

public class JWKSResult {
    List<JWKResult> keys;

    public void setKeys(List<JWKResult> keys) {
        this.keys = keys;
    }

    public List<JWKResult> getKeys() {
        return keys;
    }
}
