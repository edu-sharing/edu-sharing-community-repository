package org.edu_sharing.restservices.login.v1.model;

import jakarta.servlet.http.HttpSession;

public class ScopeLogin extends AbstractLogin {
    public ScopeLogin(boolean isValidLogin, String scope, HttpSession session) {
        super(isValidLogin, scope, session);
    }

    public ScopeLogin(boolean isValidLogin, String scope, String userHome, HttpSession session, String statusCode) {
        super(isValidLogin, scope, userHome, session, statusCode);
    }
}
