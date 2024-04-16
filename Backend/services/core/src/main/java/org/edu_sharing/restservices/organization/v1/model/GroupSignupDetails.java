package org.edu_sharing.restservices.organization.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.service.organization.GroupSignupMethod;

@Schema(description = "")
public class GroupSignupDetails {
    @JsonProperty private GroupSignupMethod signupMethod;
    @JsonProperty(required = false) private String signupPassword;

    public GroupSignupMethod getSignupMethod() {
        return signupMethod;
    }

    public void setSignupMethod(GroupSignupMethod signupMethod) {
        this.signupMethod = signupMethod;
    }

    public String getSignupPassword() {
        return signupPassword;
    }

    public void setSignupPassword(String signupPassword) {
        this.signupPassword = signupPassword;
    }
}
