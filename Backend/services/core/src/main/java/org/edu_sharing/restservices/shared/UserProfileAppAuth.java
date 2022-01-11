package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;

public class UserProfileAppAuth extends UserProfile {
    HashMap<String,String[]> extendedAttributes = new HashMap<>();

    @JsonProperty
    public HashMap<String, String[]> getExtendedAttributes() {
        return extendedAttributes;
    }
}
