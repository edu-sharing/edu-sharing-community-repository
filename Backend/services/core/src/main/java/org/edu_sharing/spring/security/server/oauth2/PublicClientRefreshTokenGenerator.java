package org.edu_sharing.spring.security.server.oauth2;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Base64;

/**
 * Wraps the token generator of the authorization server and adds the one token it refuses to produce: a
 * refresh token for a public client on the authorization code grant.
 * <p>
 * {@code OAuth2RefreshTokenGenerator} returns null for that case, so a client authenticating with
 * {@code none} - a browser extension, a mobile app - has to run the whole authorization code flow again
 * every time its access token expires. Rfc 9700 does not forbid the refresh token there, it requires it to
 * be rotated or sender constrained, so this fills the gap for clients that opt in via
 * {@code forceRefreshToken} while {@link OAuth2AuthorizationServerConfig} turns rotation on for them.
 * <p>
 * This <em>wraps</em> the default generator rather than replacing it. Registering an
 * {@code OAuth2TokenGenerator} bean would make {@code OAuth2ConfigurerUtils.getTokenGenerator} skip
 * building the default one altogether, silently dropping {@code DefaultOAuth2TokenCustomizers} - the
 * package private customizer that adds the {@code cnf} claims binding an access token to an mtls
 * certificate or a dpop key. Delegating keeps all of that intact.
 * <p>
 * Without a client opting in this is a pure pass through, which is why it is installed unconditionally:
 * the filter chain is built once at startup while the client repository is refreshable, so deciding at
 * startup whether to install it would ignore any client added later.
 * <p>
 * TODO: a refresh token handed to a public client stays valid without the user being present, and there is
 * currently no way for them to see or end that. The pieces exist - {@code /oauth2server/revoke} and the
 * authorizations in {@code oauth2_authorization} - what is missing is a "connected applications" view.
 * Every product that hands long lived tokens to clients it cannot authenticate offers one.
 */
public final class PublicClientRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {

    /**
     * key the opt-in is stored under in {@link ClientSettings}, set by
     * {@link OAuth2AuthorizationServerConfig} from {@code forceRefreshToken}. It lives on the client so
     * that it travels with it when the client repository is rebuilt on a config refresh.
     */
    public static final String SETTING_FORCE_REFRESH_TOKEN = "settings.client.edu-sharing.force-refresh-token";

    /**
     * the same 96 byte token {@code OAuth2RefreshTokenGenerator} produces
     */
    private final StringKeyGenerator refreshTokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private final OAuth2TokenGenerator<? extends OAuth2Token> delegate;

    public PublicClientRefreshTokenGenerator(OAuth2TokenGenerator<? extends OAuth2Token> delegate) {
        Assert.notNull(delegate, "delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        OAuth2Token token = this.delegate.generate(context);
        if (token != null) {
            return token;
        }
        // step in for exactly the case the default generator opts out of and nothing else, so that a null
        // for any other reason keeps surfacing as the error it is
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())
                || !isPublicClientAuthorizationCodeGrant(context)
                || !isForceRefreshToken(context.getRegisteredClient())) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
    }

    /**
     * whether the client opted in via {@code forceRefreshToken}
     */
    public static boolean isForceRefreshToken(RegisteredClient registeredClient) {
        return Boolean.TRUE.equals(registeredClient.getClientSettings().getSetting(SETTING_FORCE_REFRESH_TOKEN));
    }

    /**
     * mirrors {@code OAuth2RefreshTokenGenerator.isPublicClientForAuthorizationCodeGrant}, the condition
     * under which the default generator returns null
     */
    private static boolean isPublicClientAuthorizationCodeGrant(OAuth2TokenContext context) {
        return AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())
                && context.getAuthorizationGrant() != null
                && context.getAuthorizationGrant().getPrincipal() instanceof OAuth2ClientAuthenticationToken client
                && ClientAuthenticationMethod.NONE.equals(client.getClientAuthenticationMethod());
    }
}
