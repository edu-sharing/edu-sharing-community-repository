package org.edu_sharing.spring.security.server.oauth2.config;

import com.typesafe.config.Optional;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OAuth2Config {

    boolean enabled;
    List<Client> clients;

    /**
     * external issuers whose access tokens are accepted as bearer tokens, see
     * {@code security.authentication.oauth2.trustedIssuers} in edu-sharing.reference.conf
     */
    @Optional
    List<TrustedIssuer> trustedIssuers = new ArrayList<>();

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

    @Data
    public static final class TrustedIssuer {
        /**
         * expected iss claim, also the key the token is routed by
         */
        String issuerUri;
        /**
         * explicit jwk set uri, skips openid discovery when set
         */
        @Optional
        String jwkSetUri = "";
        /**
         * expected entry in the aud claim, binds the token to this repository as its target
         */
        @Optional
        String audience = "";
        /**
         * expected azp claim, binds the token to the client it was issued to
         */
        @Optional
        String authorizedParty = "";
        /**
         * claim carrying the edu-sharing user name, defaults to sub
         */
        @Optional
        String usernameClaim = "";
        /**
         * fixed edu-sharing user every token of this issuer is mapped to, takes precedence over usernameClaim
         */
        @Optional
        String username = "";
    }
}
