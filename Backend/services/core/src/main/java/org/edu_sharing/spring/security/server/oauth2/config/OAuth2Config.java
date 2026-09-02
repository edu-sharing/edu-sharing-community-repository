package org.edu_sharing.spring.security.server.oauth2.config;

import com.typesafe.config.Optional;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        /**
         * value of {@code clientAuthenticationMethod} marking a public client, i.e. one that cannot keep a
         * secret (browser extension, mobile app, single page app). Such a client is only safe with PKCE.
         */
        public static final String CLIENT_AUTHENTICATION_METHOD_NONE = "none";

        String clientId;
        @Optional
        String clientSecret = "";
        @Optional
        String clientAuthenticationMethod = "client_secret_basic";
        List<String> authorizationGrantTypes;
        /**
         * single redirect uri, kept for configs written before {@link #redirectUris} existed. merged with
         * that list, so both may be set
         */
        @Optional
        String redirectUri = "";
        /**
         * redirect uris matched exactly, as required by rfc 6749. the only exception is a loopback host
         * (127.0.0.0/8, ::1), where the port of the request is ignored per rfc 8252 - spring handles that
         * on its own
         */
        @Optional
        List<String> redirectUris = new ArrayList<>();
        /**
         * redirect uris matched by pattern, for clients whose redirect uri is not known at configuration
         * time. see {@code security.authentication.oauth2.clients.redirectUriPatterns} in
         * edu-sharing.reference.conf for the matching rules and why this is a deliberate relaxation of the
         * exact matching above
         */
        @Optional
        List<String> redirectUriPatterns = new ArrayList<>();
        @Optional
        List<String> scopes = new ArrayList<>();
        @Optional
        String accessTokenExpires = "";
        @Optional
        String refreshTokenExpires = "";
        @Optional
        boolean requireConsent = false;
        /**
         * whether the client must send a pkce code_challenge (rfc 7636). always on for a public client,
         * see {@link #isPublicClient()} - the code is the only thing protecting the exchange there.
         */
        @Optional
        boolean requireProofKey = false;

        /**
         * a client that authenticates with {@code none}, i.e. one that holds no secret
         */
        public boolean isPublicClient() {
            return CLIENT_AUTHENTICATION_METHOD_NONE.equals(clientAuthenticationMethod);
        }

        /**
         * @return {@link #redirectUri} and {@link #redirectUris} as one set, in configuration order
         */
        public Set<String> getAllRedirectUris() {
            Set<String> all = new LinkedHashSet<>();
            if (!redirectUri.isEmpty()) {
                all.add(redirectUri);
            }
            all.addAll(redirectUris);
            return all;
        }
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
