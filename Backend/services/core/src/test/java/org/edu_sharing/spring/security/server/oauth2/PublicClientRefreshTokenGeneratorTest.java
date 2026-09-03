package org.edu_sharing.spring.security.server.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The generator has to fill exactly one gap - a refresh token for a public client that opted in - and stay
 * out of the way everywhere else. A too eager fallback would hand refresh tokens to clients that must not
 * have one; a too narrow one leaves the opt-in without effect.
 */
class PublicClientRefreshTokenGeneratorTest {

    private static final OAuth2TokenGenerator<OAuth2Token> NO_TOKEN = context -> null;

    @Test
    @DisplayName("whatever the default generator produces is passed through untouched")
    void passesThroughTheDelegateResult() {
        OAuth2RefreshToken fromDelegate =
                new OAuth2RefreshToken("from-delegate", Instant.now(), Instant.now().plusSeconds(60));

        OAuth2Token token = new PublicClientRefreshTokenGenerator(context -> fromDelegate)
                .generate(context(publicClient(true), ClientAuthenticationMethod.NONE,
                        OAuth2TokenType.REFRESH_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE));

        assertSame(fromDelegate, token);
    }

    @Test
    @DisplayName("a public client that opted in gets the refresh token spring refuses to generate")
    void generatesForOptedInPublicClient() {
        OAuth2Token token = generate(publicClient(true), ClientAuthenticationMethod.NONE,
                OAuth2TokenType.REFRESH_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE);

        OAuth2RefreshToken refreshToken = assertInstanceOf(OAuth2RefreshToken.class, token);
        assertEquals(Duration.ofHours(1),
                Duration.between(refreshToken.getIssuedAt(), refreshToken.getExpiresAt()));
    }

    @Test
    @DisplayName("without the opt-in the public client keeps getting nothing")
    void doesNotGenerateWithoutOptIn() {
        assertNull(generate(publicClient(false), ClientAuthenticationMethod.NONE,
                OAuth2TokenType.REFRESH_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE));
    }

    @Test
    @DisplayName("a confidential client is none of our business, spring generates its refresh token")
    void doesNotGenerateForConfidentialClient() {
        assertNull(generate(confidentialClient(), ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                OAuth2TokenType.REFRESH_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE));
    }

    @Test
    @DisplayName("a missing access token stays missing - only refresh tokens are filled in")
    void doesNotGenerateAccessTokens() {
        assertNull(generate(publicClient(true), ClientAuthenticationMethod.NONE,
                OAuth2TokenType.ACCESS_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE));
    }

    @Test
    @DisplayName("on the refresh grant the default generator is competent, we must not step in")
    void doesNotGenerateOnRefreshGrant() {
        assertNull(generate(publicClient(true), ClientAuthenticationMethod.NONE,
                OAuth2TokenType.REFRESH_TOKEN, AuthorizationGrantType.REFRESH_TOKEN));
    }

    private OAuth2Token generate(RegisteredClient registeredClient, ClientAuthenticationMethod method,
                                 OAuth2TokenType tokenType, AuthorizationGrantType grantType) {
        return new PublicClientRefreshTokenGenerator(NO_TOKEN)
                .generate(context(registeredClient, method, tokenType, grantType));
    }

    private OAuth2TokenContext context(RegisteredClient registeredClient, ClientAuthenticationMethod method,
                                       OAuth2TokenType tokenType, AuthorizationGrantType grantType) {
        OAuth2ClientAuthenticationToken clientPrincipal =
                new OAuth2ClientAuthenticationToken(registeredClient, method, null);
        return DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(new TestingAuthenticationToken("admin", null))
                .tokenType(tokenType)
                .authorizationGrantType(grantType)
                .authorizationGrant(new OAuth2AuthorizationCodeAuthenticationToken("code", clientPrincipal, null, null))
                .build();
    }

    private RegisteredClient publicClient(boolean forceRefreshToken) {
        ClientSettings.Builder clientSettings = ClientSettings.builder().requireProofKey(true);
        if (forceRefreshToken) {
            clientSettings.setting(PublicClientRefreshTokenGenerator.SETTING_FORCE_REFRESH_TOKEN, true);
        }
        return client().clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .clientSettings(clientSettings.build())
                .build();
    }

    private RegisteredClient confidentialClient() {
        return client().clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientSecret("{noop}secret")
                .clientSettings(ClientSettings.builder()
                        .setting(PublicClientRefreshTokenGenerator.SETTING_FORCE_REFRESH_TOKEN, true)
                        .build())
                .build();
    }

    private RegisteredClient.Builder client() {
        return RegisteredClient.withId("browser-plugin")
                .clientId("browser-plugin")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(RedirectUriPatternValidator.PATTERN_ONLY_REDIRECT_URI)
                .scope("profile")
                // spring's default, spelled out because the test asserts on it
                .tokenSettings(TokenSettings.builder().refreshTokenTimeToLive(Duration.ofHours(1)).build());
    }
}
