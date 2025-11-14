package org.edu_sharing.restservices.login.v1.model;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@EqualsAndHashCode(callSuper = true)
public class PrimaryLogin extends AbstractLogin {

    private final List<OAuthEntry> oauthEntries;

    public PrimaryLogin(boolean isValidLogin, String scope, String userHome, HttpSession session, String statusCode,  List<OAuthEntry> oauthEntries) {
        super(isValidLogin, scope, userHome, session, statusCode);
        this.oauthEntries = oauthEntries;
    }


    public record OAuthEntry(String name, String registrationId, String clientId, boolean allowThirdPartyLoginPlugin) {
    }
}
