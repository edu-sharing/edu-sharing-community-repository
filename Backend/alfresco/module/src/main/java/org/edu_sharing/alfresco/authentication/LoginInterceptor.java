package org.edu_sharing.alfresco.authentication;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public interface LoginInterceptor {
    @Getter
    @Setter
    @Builder
    class LoginContext {
        private String authorityName;
    }

    /**
     * called when the user logged in (any method, either locally or via oidc, authByApp, ticket etc.)
     */
    void onLogin(LoginContext context);
}
