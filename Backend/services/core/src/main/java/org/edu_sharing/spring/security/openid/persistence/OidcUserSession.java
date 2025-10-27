package org.edu_sharing.spring.security.openid.persistence;

import lombok.Data;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;

@Data
public class OidcUserSession {
    private String sessionId;
    private OidcSessionInformation sessionInformation;
}
