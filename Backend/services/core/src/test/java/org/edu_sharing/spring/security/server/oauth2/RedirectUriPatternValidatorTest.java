package org.edu_sharing.spring.security.server.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectUriPatternValidatorTest {

    private static final String CHROMIUM_PATTERN = "https://*.chromiumapp.org/";

    @ParameterizedTest
    @CsvSource({
            // chrome, published extension
            "https://*.chromiumapp.org/,        https://abcdefghijklmnopabcdefghijklmnop.chromiumapp.org/",
            // firefox, uuid generated per installation
            "https://*.extensions.allizom.org/, https://1f2e3d4c-aaaa-bbbb-cccc-ddddeeeeffff.extensions.allizom.org/",
            // whole host as wildcard
            "chrome-extension://*/oauth,        chrome-extension://abcdefghij/oauth",
            // path beyond the host
            "https://*.chromiumapp.org/callback, https://abc.chromiumapp.org/callback",
            // wildcard inside a path segment
            "https://plugin.example.org/cb/*,   https://plugin.example.org/cb/instance1",
            // hosts are case insensitive
            "https://*.chromiumapp.org/,        HTTPS://ABC.CHROMIUMAPP.ORG/",
    })
    void matchesConfiguredPattern(String pattern, String requestedRedirectUri) {
        assertTrue(RedirectUriPatternValidator.matches(pattern, requestedRedirectUri));
    }

    @DisplayName("a * never crosses a component boundary")
    @ParameterizedTest
    @ValueSource(strings = {
            // the registered host must not end up somewhere in the path of an attacker controlled host
            "https://evil.org/x.chromiumapp.org/",
            // one wildcard is one label, not a whole subdomain tree
            "https://a.b.chromiumapp.org/",
            // suffix rather than subdomain
            "https://abc.chromiumapp.org.evil.org/",
            // the wildcard has to match at least something
            "https://chromiumapp.org/",
            // scheme, port, path and query are all matched exactly
            "http://abc.chromiumapp.org/",
            "https://abc.chromiumapp.org:8443/",
            "https://abc.chromiumapp.org/deeper/path",
            "https://abc.chromiumapp.org/?next=https://evil.org",
            "https://abc.chromiumapp.org",
            // a redirect uri must not carry a fragment
            "https://abc.chromiumapp.org/#/x",
            // user info keeps the host but changes what the url looks like to a user
            "https://evil.org@abc.chromiumapp.org/",
            // the pattern itself must not be usable as a redirect uri
            "https://*.chromiumapp.org/",
    })
    void doesNotMatchConfiguredPattern(String requestedRedirectUri) {
        assertFalse(RedirectUriPatternValidator.matches(CHROMIUM_PATTERN, requestedRedirectUri));
    }

    @DisplayName("a pattern without a wildcard still has to match exactly")
    @ParameterizedTest
    @CsvSource({
            "https://plugin.example.org/cb, https://plugin.example.org/cb,     true",
            "https://plugin.example.org/cb, https://plugin.example.org/cb/sub, false",
            "https://plugin.example.org/cb, https://plugin.example.org/,       false",
    })
    void matchesLiteralPattern(String pattern, String requestedRedirectUri, boolean expected) {
        assertEquals(expected, RedirectUriPatternValidator.matches(pattern, requestedRedirectUri));
    }

    /**
     * Setting a validator on the provider replaces the composition spring security built in its own
     * init, so the rule it adds while openid connect is disabled has to be carried along. Losing it means
     * a client asking for openid silently gets an access token and no id_token.
     */
    @Test
    @DisplayName("the openid scope stays rejected while openid connect is disabled")
    void rejectsOpenidScope() {
        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> validator =
                RedirectUriPatternValidator.authorizationEndpointValidator();

        OAuth2AuthorizationCodeRequestAuthenticationException e = assertThrows(
                OAuth2AuthorizationCodeRequestAuthenticationException.class,
                () -> validator.accept(context(Set.of(OidcScopes.OPENID, "profile"))));

        assertEquals(OAuth2ErrorCodes.INVALID_SCOPE, e.getError().getErrorCode());
    }

    @Test
    @DisplayName("a request matching a pattern passes the whole composed validator")
    void acceptsPatternMatchWithoutOpenidScope() {
        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> validator =
                RedirectUriPatternValidator.authorizationEndpointValidator();

        assertDoesNotThrow(() -> validator.accept(context(Set.of("profile"))));
    }

    /**
     * a public client whose redirect uri is only covered by a pattern, asking for the given scopes with
     * the redirect uri a chrome extension actually sends
     */
    private OAuth2AuthorizationCodeRequestAuthenticationContext context(Set<String> requestedScopes) {
        String requestedRedirectUri = "https://bdpcahnkimdbloipcandiohimejodhmk.chromiumapp.org/";
        RegisteredClient registeredClient = RegisteredClient.withId("browser-plugin")
                .clientId("browser-plugin")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(RedirectUriPatternValidator.PATTERN_ONLY_REDIRECT_URI)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .setting(RedirectUriPatternValidator.SETTING_REDIRECT_URI_PATTERNS,
                                List.of(CHROMIUM_PATTERN))
                        .build())
                .scope(OidcScopes.OPENID)
                .scope("profile")
                .build();
        OAuth2AuthorizationCodeRequestAuthenticationToken authentication =
                new OAuth2AuthorizationCodeRequestAuthenticationToken("/oauth2server/authorize", "browser-plugin",
                        new TestingAuthenticationToken("admin", null), requestedRedirectUri, "state",
                        requestedScopes, null);
        return OAuth2AuthorizationCodeRequestAuthenticationContext.with(authentication)
                .registeredClient(registeredClient)
                .build();
    }
}
