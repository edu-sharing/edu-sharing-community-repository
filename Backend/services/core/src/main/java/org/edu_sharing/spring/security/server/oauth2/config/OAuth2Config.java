package org.edu_sharing.spring.security.server.oauth2.config;

import lombok.Data;

import java.util.List;

@Data
public class OAuth2Config {

    boolean enabled;
    List<Client> clients;

    @Data
    public static final class Client {
        String clientId;
        String clientSecret;
        String clientAuthenticationMethod;
        List<String> authorizationGrantTypes;
        String redirectUri;
        List<String> scopes;
        String accessTokenExpires;
        String refreshTokenExpires;
        boolean requireConsent;
        // Spring Authorization Server 7.0 enables PKCE by default; keep it opt-in here so existing
        // confidential clients that do not send a code_challenge keep working. Set to true to require PKCE.
        boolean requireProofKey;
    }
}
